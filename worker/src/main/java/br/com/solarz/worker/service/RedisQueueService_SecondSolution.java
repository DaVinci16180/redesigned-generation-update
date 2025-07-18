package br.com.solarz.worker.service;

import br.com.solarz.worker.config.RedisClientProvider;
import br.com.solarz.worker.model.ApiScore;
import br.com.solarz.worker.model.Usina;
import br.com.solarz.worker.repository.ApiRepository;
import br.com.solarz.worker.repository.ApiScoreRepository;
import br.com.solarz.worker.repository.UsinaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RedisQueueService_SecondSolution {

    private final ApiScoreRepository apiScoreRepository;
    private RedissonClient redissonClient;

    private final RedisClientProvider redisClientProvider;
    private final UsinaRepository usinaRepository;
    private final ApiRepository apiRepository;
    private final MeterRegistry meterRegistry;

    Map<Long, Double> apiErrorRateScores = new HashMap<>();

    @PostConstruct
    public void setup() {
        this.redissonClient = redisClientProvider.getClient();
        List<ApiScore> scores = apiScoreRepository.findAll();
        scores.sort(Comparator.comparingDouble(ApiScore::getErrorRate));

        for (int i = 0; i < scores.size(); i++) {
            ApiScore score = scores.get(i);
            apiErrorRateScores.put(score.getId(), (double) i);
        }
    }

    public synchronized List<Usina> dequeue(int amount) {
        RScoredSortedSet<Long> queue =  redissonClient.getScoredSortedSet("queue_second_solution");
        return usinaRepository.findAllById(queue.pollFirst(amount));
    }

    public void queueFailed(Usina usina) {
        RScoredSortedSet<Long> queue =  redissonClient.getScoredSortedSet("queue_second_solution");

        long apiId = usina.getCredencial().getApi().getId();
        int offset = apiErrorRateScores.size() * 2;
        double score = apiErrorRateScores.get(apiId) + offset;
        queue.add(score, usina.getId());
    }
}
