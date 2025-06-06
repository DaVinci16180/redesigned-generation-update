package br.com.solarz.worker.service;

import br.com.solarz.worker.config.RedisClientProvider;
import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.model.Usina;
import br.com.solarz.worker.model.Usina.Priority;
import br.com.solarz.worker.repository.ApiRepository;
import br.com.solarz.worker.repository.CredencialRepository;
import br.com.solarz.worker.repository.UsinaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RedisQueueService {

    public enum QueueType {
        AVAILABLE, // fila usinas disponíveis para atualização
        FAILED, // fila usinas que falharam a atualização
        AVAILABLE_OR_FAILED //usado para pegar dados das duas filas ao mesmo tempo, priorizando a fila available
    };

    private HashMap<String, HashMap<Integer, RSet<Long>>> queues = new HashMap<>();
    private RedissonClient redissonClient;

    private final CredencialRepository credencialRepository;
    private final UsinaRepository usinaRepository;
    private final ApiRepository apiRepository;

    @PostConstruct
    public void setup() {
        this.redissonClient = RedisClientProvider.getClient();
        setupQueues();
    }

    public void setupQueues() {
        List<Api> apis = apiRepository.findAll();
        for (Api api : apis) {
            String avaQueueName = buildName(api, QueueType.AVAILABLE);
            HashMap<Integer, RSet<Long>> available = new HashMap<>();
            available.put(Priority.HIGH.ordinal(), redissonClient.getSet(avaQueueName + "_" + Priority.HIGH));
            available.put(Priority.NORMAL.ordinal(), redissonClient.getSet(avaQueueName + "_" + Priority.NORMAL));

            String errQueueName = buildName(api, QueueType.FAILED);
            HashMap<Integer, RSet<Long>> error = new HashMap<>();
            error.put(Priority.HIGH.ordinal(), redissonClient.getSet(errQueueName + "_" + Priority.HIGH));
            error.put(Priority.NORMAL.ordinal(), redissonClient.getSet(errQueueName + "_" + Priority.NORMAL));

            queues.put(avaQueueName, available);
            queues.put(errQueueName, error);
        }
    }

    public synchronized Set<Usina> dequeue(String queueName, int amount, Priority priority) {
        Set<Usina> usinas = new HashSet<>();
        var queueWithPriority = queues.get(queueName);

        if (priority != null) {
            RSet<Long> queue = queueWithPriority.get(priority.ordinal());
            Set<Long> ids = queue.removeRandom(amount);
            return new HashSet<>(usinaRepository.findAllById(ids));
        }

        for (RSet<Long> queue : queueWithPriority.values()) {
            if (amount - usinas.size() <= 0)
                break;

            Set<Long> ids = queue.removeRandom(amount - usinas.size());

            if (ids.isEmpty())
                continue;

            usinas.addAll(usinaRepository.findAllById(ids));
        }

        return usinas;
    }

    public Set<Usina> getUsinasByApi(Api api, QueueType type, int amount, Priority priority) {
        if (type == QueueType.AVAILABLE_OR_FAILED) {
            String avaQueueName = buildName(api, QueueType.AVAILABLE);
            String errQueueName = buildName(api, QueueType.FAILED);

            Set<Usina> usinas = dequeue(avaQueueName, amount, priority);
            if (usinas.size() < amount) {
                Set<Usina> failed = dequeue(errQueueName, amount - usinas.size(), priority);
                usinas.addAll(failed);
            }

            return usinas;
        }

        String queueName = buildName(api, type);
        return dequeue(queueName, amount, priority);
    }

    public String buildName(Api api, QueueType type) {
        return api.getName() + "_" + type;
    }
}
