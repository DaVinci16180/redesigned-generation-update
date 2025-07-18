package br.com.solarz.master.service;

import br.com.solarz.master.MasterApplication;
import config.RedisClientProvider;
import model.ApiScore;
import model.Credencial;
import model.Usina;
import repository.ApiScoreRepository;
import repository.CredencialRepository;
import repository.UsinaRepository;
import br.com.solarz.master.scheduler.MetricsScheduler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueBuilderService_SecondSolution {

    Logger logger = LoggerFactory.getLogger(QueueBuilderService_Original.class);
    private RedissonClient redissonClient;
    private final RestClient client = RestClient.create();

    private final CredencialRepository credencialRepository;
    private final RedisClientProvider redisClientProvider;
    private final ApiScoreRepository apiScoreRepository;
    private final UsinaRepository usinaRepository;

    @PostConstruct
    public void setup() {
        this.redissonClient = redisClientProvider.getClient();

        List<ApiScore> scores = apiScoreRepository.findAll();
        apiScoreRepository.saveAll(scores.stream().peek(s -> s.setPending(1)).toList());

        buildQueues();
        startSim();
    }

    public void buildQueues() {
        Instant start = Instant.now();

        RScoredSortedSet<Long> queue =  redissonClient.getScoredSortedSet("queue_second_solution");
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

                double finalI = i;
                Map<Long, Double> usinasHigh = usinas
                        .stream()
                        .filter(u -> u.getPriority().equals(Usina.Priority.HIGH))
                        .collect(Collectors.toMap(Usina::getId, x -> finalI));

                Map<Long, Double> usinasNorm = usinas
                        .stream()
                        .filter(u -> u.getPriority().equals(Usina.Priority.NORMAL))
                        .collect(Collectors.toMap(Usina::getId, x -> finalI + scores.size()));

                queue.addAll(usinasHigh);
                queue.addAll(usinasNorm);
            }
        }

        Instant finish = Instant.now();
        Duration duration = Duration.between(start, finish);

        System.out.println("Building queues took " + duration.toSeconds() + " seconds");
    }

    public void startSim() {
        Map<String, String> body = new HashMap<>();
        body.put("operation", "start");

        for (String addr : MasterApplication.WORKERS_ADDR) {
            client.post()
                    .uri("http://" + addr + ":8081/simulation/change-state")
                    .body(body)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        }

        if (MetricsScheduler.start == null)
            MetricsScheduler.start = Instant.now();

        logger.info("Simulation started");

        try {
            FileOutputStream fos = new FileOutputStream("logs.txt", true);
            fos.write(("\n" + ZonedDateTime.now() + " - Simulation started\n").getBytes());
            fos.close();
        } catch (IOException ignored) {}
    }
}
