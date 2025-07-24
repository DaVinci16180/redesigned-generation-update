package br.com.solarz.worker.service;

import io.micrometer.core.instrument.Gauge;
import model.Api;
import model.Usina;
import model.Usina.Priority;
import repository.ApiRepository;
import repository.UsinaRepository;
import br.com.solarz.worker.scheduler.GenerationUpdateScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import br.com.solarz.worker.service.CompositeQueueService.QueueType;

@Primary
@Service("original")
@RequiredArgsConstructor
public class GenerationUpdateService {

    private final CompositeQueueService compositeQueueService;
    private final SingleQueueService singleQueueService;
    private final UsinaRepository usinaRepository;
    private final MeterRegistry meterRegistry;
    private final ApiRepository apiRepository;
    private OkHttpClient client;

    @Value("${DOCKER_ADDR}")
    private String DOCKER_ADDR;
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

        buildMeters();
    }

    private void buildMeters() {
//        List<Api> apis = apiRepository.findAll();
//
//        for (Api api : apis) {
//            threadCounter.put(api, 0);
//
//            int usinasAmount = usinaRepository.countByCredencial_Api(api);
//            averages.put(api, new ApiAverages(usinasAmount));
//        }

        Gauge.builder("simulacao.usinas.pending", usinaRepository,
                        ur -> ur.countByUpdatedAndPriority(false, Priority.HIGH))
                .tags("priority", "HIGH")
                .register(meterRegistry);

        Gauge.builder("simulacao.usinas.pending", usinaRepository,
                        ur -> ur.countByUpdatedAndPriority(false, Priority.NORMAL))
                .tags("priority", "NORMAL")
                .register(meterRegistry);
    }

    @Async("generationUpdate")
    public void updateGenerationByApi(Api api) {
        int batchSize = 20;
        int failedRecap = 5; // alta prioridade apenas

        Set<Usina> usinas = compositeQueueService.getUsinasByApi(api, QueueType.AVAILABLE_OR_FAILED, batchSize, null);
        Set<Usina> recap = compositeQueueService.getUsinasByApi(api, QueueType.FAILED, failedRecap, Priority.HIGH);
        usinas.addAll(recap);

        if (usinas.isEmpty())
            return;

        System.out.println("Iniciando atualização do portal " + api.getName());

        List<Usina> failed = new ArrayList<>();
        for (Usina usina : usinas) {
            boolean success = updateUsinaGeneration(usina);

            if (!success) {
                failed.add(usina);
                meterRegistry.counter("simulacao.usinas.falhas").increment();
            } else {
                usina.setUpdated(true);
                usinaRepository.save(usina);
            }
        }

        compositeQueueService.queueFailed(failed, api);

        System.out.println(usinas.size() + " usinas do portal " + api.getName() + " processadas. Falhas: " + failed.size());
    }

    @Async("generationUpdate")
    public void updateGeneration() {
        List<Usina> usinas = singleQueueService.dequeue(25);
        if (usinas.isEmpty())
            return;

        for (Usina usina : usinas) {
            System.out.println("Atualizando usina " + usina.getId());

            boolean success = updateUsinaGeneration(usina);

            if (!success) {
                System.out.println("Falha na atualização da usina " + usina.getId());
                singleQueueService.queueFailed(usina);
                meterRegistry.counter("simulacao.usinas.falhas").increment();
            } else {
                System.out.println("Usina " + usina.getId() + " atualizada com sucesso");
                usina.setUpdated(true);
                usinaRepository.save(usina);
            }
        }
    }

    private boolean updateUsinaGeneration(Usina usina) {
        Request request = new Request.Builder()
                .url(API_SIM_URL + "/portal/generation?portalId=" + usina.getCredencial().getApi().getId())
                .build();

        Call call = client.newCall(request);

        try (Response response = call.execute()) {
            if (!response.isSuccessful())
                throw new RuntimeException();

            meterRegistry.counter("simulacao.usinas.processadas").increment();

            return true;
        } catch (Exception e) {
            int MAX_UPDATE_ATTEMPTS = 10;

            usina.incrementUpdateAttempts();
            usina = usinaRepository.save(usina);

            if (usina.getUpdateAttempts() >= MAX_UPDATE_ATTEMPTS)
                meterRegistry.counter("simulacao.usinas.expiradas").increment();

            return usina.getUpdateAttempts() >= MAX_UPDATE_ATTEMPTS;
        }
    }
}
