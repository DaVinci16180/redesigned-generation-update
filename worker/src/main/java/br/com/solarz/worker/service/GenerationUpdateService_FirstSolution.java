package br.com.solarz.worker.service;

import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.model.ApiScore;
import br.com.solarz.worker.model.Usina;
import br.com.solarz.worker.model.Usina.Priority;
import br.com.solarz.worker.repository.ApiScoreRepository;
import br.com.solarz.worker.repository.UsinaRepository;
import br.com.solarz.worker.scheduler.GenerationUpdateScheduler;
import br.com.solarz.worker.util.ApiAverages;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import br.com.solarz.worker.service.RedisQueueService.QueueType;

@Service("first")
@RequiredArgsConstructor
public class GenerationUpdateService_FirstSolution implements GenerationUpdateInterface {

    private final RedisQueueService redisQueueService;
    private final UsinaRepository usinaRepository;
    private final MeterRegistry meterRegistry;
    private final ApiScoreRepository apiScoreRepository;
    private OkHttpClient client;

    @Value("${DOCKER_ADDR}")
    private String DOCKER_ADDR;

    private final HashMap<Api, Integer> threadCounter = new HashMap<>();
    private final HashMap<Api, ApiAverages> averages = new HashMap<>();
    private String API_SIM_URL;

    @PostConstruct
    public void setup() {
        client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .build();

        API_SIM_URL = "http://" + DOCKER_ADDR + ":8082";
    }

    @Async("generationUpdate")
    public void updateGenerationByApi(Api api, ApiScore score) {
        // checar se está dentro da janela de atualização
        if (!GenerationUpdateScheduler.RUNNING)
            return;

        if (!averages.containsKey(api)) {
            int usinasAmount = usinaRepository.countByApiId(api.getId());
            averages.put(api, new ApiAverages(usinasAmount));
        }

        ApiAverages average = averages.get(api);

        Instant start = Instant.now();

        System.out.println("Iniciando atualização do portal " + api.getName());

        threadCounter.put(api, threadCounter.getOrDefault(api, 0) + 1);
        int batchSize = 20;
        int failedRecap = 5; // alta prioridade apenas

        Set<Usina> usinas = redisQueueService.getUsinasByApi(api, QueueType.AVAILABLE_OR_FAILED, batchSize, null);
        Set<Usina> recap = redisQueueService.getUsinasByApi(api, QueueType.FAILED, failedRecap, Priority.HIGH);
        usinas.addAll(recap);

        List<Usina> failed = new ArrayList<>();
        for (Usina usina : usinas) {
            boolean success = updateUsinaGeneration(usina, average);

            if (!success) {
                failed.add(usina);
                meterRegistry.counter("simulacao.usinas.falhas").increment();
            }
        }

        redisQueueService.queueFailed(failed, api);

        Instant finish = Instant.now();
        Duration duration = Duration.between(start, finish);

        System.out.println(usinas.size() + " usinas do portal " + api.getName() + " atualizadas em " + duration.toMillis() + " milissegundos. Falhas: " + failed.size());

//        if (score.getAverageTime() == .0 || average.isFull()) {
        score.setAverageTime(average.averageTime());
        score.setErrorRate(average.errorRate());
//        }

        int notUpdated = usinaRepository.countNotUpdatedByApiId(api.getId());
        score.setPending( notUpdated / (double) average.getUsinasAmount());

        apiScoreRepository.save(score);

        threadCounter.put(api, threadCounter.get(api) - 1);
    }

    private boolean updateUsinaGeneration(Usina usina, ApiAverages average) {
        int MAX_UPDATE_ATTEMPTS = 10;

        boolean success;
        Instant start = Instant.now();

        Request request = new Request.Builder()
                .url(API_SIM_URL + "/portal/generation?portalId=" + usina.getCredencial().getApi().getId())
                .build();

        Call call = client.newCall(request);

        try (Response response = call.execute()) {
            if (!response.isSuccessful())
                throw new RuntimeException();

            usina.setUpdated(true);
            usinaRepository.save(usina);

            meterRegistry.counter("simulacao.usinas.processadas").increment();

            success = true;
        } catch (Exception e) {
            usina.incrementUpdateAttempts();
            usina = usinaRepository.save(usina);

            success = false;
        }

        Instant finish = Instant.now();
        Duration duration = Duration.between(start, finish);

        average.register(duration.toMillis(), !success);

        return success || usina.getUpdateAttempts() >= MAX_UPDATE_ATTEMPTS;
    }
}
