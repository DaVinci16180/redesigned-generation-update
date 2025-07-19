package br.com.solarz.worker.service;

import config.RedisClientProvider;
import model.Api;
import model.ApiScore;
import model.Usina;
import org.redisson.api.RScoredSortedSet;
import repository.ApiScoreRepository;
import repository.UsinaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RedisQueueService {

    private RedissonClient redissonClient;

    private final RedisClientProvider redisClientProvider;
    private final ApiScoreRepository apiScoreRepository;
    private final UsinaRepository usinaRepository;
    private final MeterRegistry meterRegistry;

    RScoredSortedSet<Long> queue;
    Map<Long, Double> availableScores = new HashMap<>();
    Map<Long, Double> errorScores = new HashMap<>();

    @PostConstruct
    public void setup() {
        redissonClient = redisClientProvider.getClient();
        queue = redissonClient.getScoredSortedSet("usinas_queue");

        buildMeters();
    }

    private void buildMeters() {
        List<ApiScore> apiScores = apiScoreRepository.findAll();

        apiScores.sort(Comparator.comparingDouble(ApiScore::calculate));
        for (int i = 0; i < apiScores.size(); i++) {
            ApiScore apiScore = apiScores.get(i);
            Api api = apiScore.getApi();

            double scoreHigh = i;
            double scoreNormal = i + apiScores.size();

            availableScores.put(apiScore.getId(), scoreHigh);

            Gauge.builder("available.queue.size", queue, q -> q.count(scoreHigh, true, scoreHigh, true))
                    .tags("portal", api.getName(), "priority", "HIGH")
                    .register(meterRegistry);

            Gauge.builder("available.queue.size", queue, q -> q.count(scoreNormal, true, scoreNormal, true))
                    .tags("portal", api.getName(), "priority", "NORMAL")
                    .register(meterRegistry);
        }

        apiScores.sort(Comparator.comparingDouble(ApiScore::getErrorRate));
        for (int i = 0; i < apiScores.size(); i++) {
            ApiScore apiScore = apiScores.get(i);
            Api api = apiScore.getApi();

            double scoreHigh = i + apiScores.size() * 2;
            double scoreNormal = i + apiScores.size() * 3;

            errorScores.put(apiScore.getId(), (double) i);

            Gauge.builder("error.queue.size", queue, q -> q.count(scoreHigh, true, scoreHigh, true))
                    .tags("portal", api.getName(), "priority", "HIGH")
                    .register(meterRegistry);

            Gauge.builder("error.queue.size", queue, q -> q.count(scoreNormal, true, scoreNormal, true))
                    .tags("portal", api.getName(), "priority", "NORMAL")
                    .register(meterRegistry);
        }
    }

    public synchronized List<Usina> dequeue(int amount) {
        return usinaRepository.findAllById(queue.pollFirst(amount));
    }

    public void queueFailed(Usina usina) {
        long apiId = usina.getCredencial().getApi().getId();
        double score = errorScores.get(apiId) + usina.getUpdateAttempts() - 1;
        queue.add(score, usina.getId());
    }
}
