package br.com.solarz.worker.service;

import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.model.Usina;
import br.com.solarz.worker.model.Usina.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import br.com.solarz.worker.service.RedisQueueService.QueueType;

@Service
@RequiredArgsConstructor
public class GenerationUpdateService {

    private final RedisQueueService redisQueueService;

    private final HashMap<Api, Integer> threadCounter = new HashMap<>();

    public void updateGenerationByApi(Api api) {
        // checar se está dentro da janela de atualização

        Instant start = Instant.now();

        threadCounter.put(api, threadCounter.getOrDefault(api, 0) + 1);
        int batchSize = 20;
        int failedRecap = 5; // alta prioridade apenas

        Set<Usina> usinas = redisQueueService.getUsinasByApi(api, QueueType.AVAILABLE_OR_FAILED, batchSize, null);
        Set<Usina> recap = redisQueueService.getUsinasByApi(api, QueueType.FAILED, failedRecap, Priority.HIGH);
        usinas.addAll(recap);

        for (Usina usina : usinas) {
            // atualiza a usina
            // registra sucesso ou falha
        }

        // reenfileira usinas que falharam

        Instant finish = Instant.now();

        threadCounter.put(api, threadCounter.get(api) - 1);
    }
}
