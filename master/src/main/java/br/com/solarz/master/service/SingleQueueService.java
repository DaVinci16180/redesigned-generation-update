package br.com.solarz.master.service;

import config.RedisClientProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import model.ApiScore;
import model.Credencial;
import model.Usina;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import repository.ApiScoreRepository;
import repository.CredencialRepository;
import repository.UsinaRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SingleQueueService {

    private RedissonClient redissonClient;

    private final CredencialRepository credencialRepository;
    private final RedisClientProvider redisClientProvider;
    private final ApiScoreRepository apiScoreRepository;
    private final UsinaRepository usinaRepository;

    @PostConstruct
    public void setup() {
        this.redissonClient = redisClientProvider.getClient();
    }

    public void buildQueues(double offsetHigh, double offsetNorm) {
        Instant start = Instant.now();

        RScoredSortedSet<Long> queue =  redissonClient.getScoredSortedSet("usinas_queue");
        queue.clear();

        List<ApiScore> scores = apiScoreRepository
                .findAll()
                .stream()
                .sorted()
                .toList();

        // AVAILABLE_HIGH -> AVAILABLE_NORMAL -> ERROR_HIGH -> ERROR_NORMAL
        for (int i = 0; i < scores.size(); i++) {
            ApiScore score = scores.get(i);
            List<Credencial> credenciais = credencialRepository.findAllByApi(score.getApi());

            for (Credencial credencial : credenciais) {
                List<Usina> usinas = usinaRepository.findAllByCredencial(credencial);
                usinas = usinas.stream().peek(Usina::reset).toList();
                usinaRepository.saveAll(usinas);

                int finalI = i;
                Map<Long, Double> usinasHigh = usinas
                        .stream()
                        .filter(u -> u.getPriority().equals(Usina.Priority.HIGH))
                        .collect(Collectors.toMap(Usina::getId, x -> finalI + offsetHigh * scores.size()));

                Map<Long, Double> usinasNorm = usinas
                        .stream()
                        .filter(u -> u.getPriority().equals(Usina.Priority.NORMAL))
                        .collect(Collectors.toMap(Usina::getId, x -> finalI + offsetNorm * scores.size()));

                queue.addAll(usinasHigh);
                queue.addAll(usinasNorm);
            }
        }

        Instant finish = Instant.now();
        Duration duration = Duration.between(start, finish);

        System.out.println("Building queues took " + duration.toSeconds() + " seconds");
    }
}
