package br.com.solarz.worker.service;

import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.model.Usina;
import br.com.solarz.worker.repository.ApiRepository;
import br.com.solarz.worker.repository.ApiScoreRepository;
import br.com.solarz.worker.repository.UsinaRepository;
import br.com.solarz.worker.scheduler.GenerationUpdateScheduler;
import br.com.solarz.worker.util.ApiAverages;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GenerationUpdateService_SecondSolution {

    private final RedisQueueService_SecondSolution redisQueueService;
    private final UsinaRepository usinaRepository;
    private final MeterRegistry meterRegistry;
    private final ApiScoreRepository apiScoreRepository;
    private final ApiRepository apiRepository;
    private OkHttpClient client;

    @Value("${DOCKER_ADDR}")
    private String DOCKER_ADDR;

    public static final HashMap<Api, ApiAverages> averages = new HashMap<>();
    private final HashMap<Api, Integer> threadCounter = new HashMap<>();
    private String API_SIM_URL;

    @PostConstruct
    public void setup() {
        API_SIM_URL = "http://" + DOCKER_ADDR + ":8082";

        client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .build();

        buildMeters();
    }

    private void buildMeters() {
        List<Api> apis = apiRepository.findAll();

        for (Api api : apis) {
            threadCounter.put(api, 0);

            Gauge.builder("thread.count", threadCounter, tc -> tc.get(api))
                    .tags("portal", api.getName())
                    .register(meterRegistry);
        }
    }

    @Async("generationUpdate")
    public void updateGeneration() {
        // checar se está dentro da janela de atualização
        if (!GenerationUpdateScheduler.RUNNING)
            return;

        List<Usina> usinas = redisQueueService.dequeue(25);
        if (usinas.isEmpty()) {
            GenerationUpdateScheduler.RUNNING = false;
            return;
        }

        for (Usina usina : usinas) {
            System.out.println("Atualizando usina " + usina.getId());

            boolean success = updateUsinaGeneration(usina);

            if (!success) {
                System.out.println("Falha na atualização da usina " + usina.getId());
                redisQueueService.queueFailed(usina);
                meterRegistry.counter("simulacao.usinas.falhas").increment();
            } else {
                System.out.println("Usina " + usina.getId() + " atualizada com sucesso");
                usina.setUpdated(true);
                usinaRepository.save(usina);
            }
        }
    }

    private boolean updateUsinaGeneration(Usina usina) {
        int MAX_UPDATE_ATTEMPTS = 10;

        boolean success;
//        Instant start = Instant.now();

        Request request = new Request.Builder()
                .url(API_SIM_URL + "/portal/generation?portalId=" + usina.getCredencial().getApi().getId())
                .build();

        Call call = client.newCall(request);

        try (Response response = call.execute()) {
            if (!response.isSuccessful())
                throw new RuntimeException();

            meterRegistry.counter("simulacao.usinas.processadas").increment();

            success = true;
        } catch (Exception e) {
            usina.incrementUpdateAttempts();
            usina = usinaRepository.save(usina);

            success = false;
        }

//        Instant finish = Instant.now();
//        Duration duration = Duration.between(start, finish);

//        average.register(duration.toMillis(), !success);

        if (usina.getUpdateAttempts() >= MAX_UPDATE_ATTEMPTS)
            meterRegistry.counter("simulacao.usinas.expiradas").increment();

        return success || usina.getUpdateAttempts() >= MAX_UPDATE_ATTEMPTS;
    }
}
