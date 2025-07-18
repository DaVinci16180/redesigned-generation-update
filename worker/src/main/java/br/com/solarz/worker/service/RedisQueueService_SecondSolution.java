package br.com.solarz.worker.service;

import br.com.solarz.worker.config.RedisClientProvider;
import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.model.ApiScore;
import br.com.solarz.worker.model.Usina;
import br.com.solarz.worker.repository.ApiRepository;
import br.com.solarz.worker.repository.ApiScoreRepository;
import br.com.solarz.worker.repository.UsinaRepository;
import io.micrometer.core.instrument.Gauge;
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

    RScoredSortedSet<Long> queue;
    Map<Long, Double> availableScores = new HashMap<>();
    Map<Long, Double> errorScores = new HashMap<>();

    @PostConstruct
    public void setup() {
        redissonClient = redisClientProvider.getClient();
        queue = redissonClient.getScoredSortedSet("queue_second_solution");

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
        int offset = errorScores.size() * (usina.getPriority().equals(Usina.Priority.HIGH) ? 2 : 3);
        double score = errorScores.get(apiId) + offset;
        queue.add(score, usina.getId());
    }
}
