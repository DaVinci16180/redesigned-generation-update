package br.com.solarz.worker.service;

import br.com.solarz.worker.scheduler.GenerationUpdateScheduler;
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
public class SingleQueueService {

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
            Map<String, Double> scores = queueScores(i);

            double scoreHigh = scores.get("AVAILABLE_HIGH");
            double scoreNormal = scores.get("AVAILABLE_NORMAL");

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
            Map<String, Double> scores = queueScores(i);

            double scoreHigh = scores.get("FAILED_HIGH");
            double scoreNormal = scores.get("FAILED_NORMAL");

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
        double rank = errorScores.get(apiId);
        Map<String, Double> scores = queueScores(rank);

        double scoreHigh = scores.get("FAILED_HIGH");
        double scoreNormal = scores.get("FAILED_NORMAL");

        if (usina.getPriority().equals(Usina.Priority.HIGH))
            queue.add(scoreHigh, usina.getId());
        else
            queue.add(scoreNormal, usina.getId());
    }

    private Map<String, Double> queueScores(double rank) {
        return switch (GenerationUpdateScheduler.solution) {
            case ORIGINAL_SOLUTION, SOLUTION_1 -> Map.of();
            case SOLUTION_2 -> solution2(rank);
            case SOLUTION_3 -> solution3(rank);
            case SOLUTION_4 -> solution4(rank);
        };
    }

    private Map<String, Double> solution2(double rank) {
        List<ApiScore> apiScores = apiScoreRepository.findAll();
        return Map.of(
                "AVAILABLE_HIGH",   rank,
                "AVAILABLE_NORMAL", rank + apiScores.size(),
                "FAILED_HIGH",      rank + apiScores.size() * 2,
                "FAILED_NORMAL",    rank + apiScores.size() * 3
        );
    }

    private Map<String, Double> solution3(double rank) {
        List<ApiScore> apiScores = apiScoreRepository.findAll();
        return Map.of(
                "AVAILABLE_HIGH",   rank,
                "AVAILABLE_NORMAL", rank + apiScores.size(),
                "FAILED_HIGH",      Math.min(queue.firstScore(), availableScores.size() * 2 - 1),
                "FAILED_NORMAL",    rank + apiScores.size() * 2
        );
    }

    private Map<String, Double> solution4(double rank) {
        List<ApiScore> apiScores = apiScoreRepository.findAll();
        return Map.of(
                "AVAILABLE_HIGH",   rank,
                "AVAILABLE_NORMAL", rank + apiScores.size() * 2,
                "FAILED_HIGH",      rank + apiScores.size(),
                "FAILED_NORMAL",    rank + apiScores.size() * 3
        );
    }
}
