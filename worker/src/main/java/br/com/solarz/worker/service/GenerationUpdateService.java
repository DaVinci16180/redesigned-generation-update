package br.com.solarz.worker.service;

import br.com.solarz.worker.WorkerApplication;
import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.model.Usina;
import br.com.solarz.worker.model.Usina.Priority;
import br.com.solarz.worker.repository.UsinaRepository;
import br.com.solarz.worker.scheduler.GenerationUpdateScheduler;
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

@Service
@RequiredArgsConstructor
public class GenerationUpdateService {

    private final RedisQueueService redisQueueService;
    private final UsinaRepository usinaRepository;
    private final MeterRegistry meterRegistry;
    private OkHttpClient client;

    @Value("${MASTER_ADDR}")
    private String MASTER_ADDR;

    private final HashMap<Api, Integer> threadCounter = new HashMap<>();
    private final String API_SIM_URL = "http://" + MASTER_ADDR + ":8082";

    @PostConstruct
    public void setup() {
        client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .build();
    }

    @Async("generationUpdate")
    public void updateGenerationByApi(Api api) {
        // checar se está dentro da janela de atualização
        if (!GenerationUpdateScheduler.RUNNING)
            return;

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
            boolean success = updateUsinaGeneration(usina);

            if (!success) {
                failed.add(usina);
                meterRegistry.counter("simulacao.usinas.falhas").increment();
            }
        }

        redisQueueService.queueFailed(failed, api);

        Instant finish = Instant.now();
        Duration duration = Duration.between(start, finish);

        System.out.println(usinas.size() + " usinas do portal " + api.getName() + " atualizadas em " + duration.toMillis() + " milissegundos. Falhas: " + failed.size());

        threadCounter.put(api, threadCounter.get(api) - 1);
    }

    private boolean updateUsinaGeneration(Usina usina) {
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

            return true;
        } catch (Exception e) {
            int MAX_UPDATE_ATTEMPTS = 10;

            usina.incrementUpdateAttempts();
            usina = usinaRepository.save(usina);

            return usina.getUpdateAttempts() >= MAX_UPDATE_ATTEMPTS;
        }
    }
}
