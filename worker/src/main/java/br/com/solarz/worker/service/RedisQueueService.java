package br.com.solarz.worker.service;

import br.com.solarz.worker.config.RedisClientProvider;
import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.model.Usina;
import br.com.solarz.worker.model.Usina.Priority;
import br.com.solarz.worker.repository.ApiRepository;
import br.com.solarz.worker.repository.CredencialRepository;
import br.com.solarz.worker.repository.UsinaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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

    private final HashMap<String, HashMap<Integer, RSet<Long>>> queues = new HashMap<>();
    private RedissonClient redissonClient;

    private final UsinaRepository usinaRepository;
    private final ApiRepository apiRepository;
    private final MeterRegistry meterRegistry;

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

            buildMeters(api);
        }
    }

    private void buildMeters(Api api) {
        String avaQueueName = buildName(api, QueueType.AVAILABLE);
        String errQueueName = buildName(api, QueueType.FAILED);

        for (var priority : queues.get(avaQueueName).keySet()) {
            String priorityName = Priority.values()[priority].name();

            Gauge.builder("available.queue.size", queues, queues -> queues.get(avaQueueName).get(priority).size())
                    .tags("portal", api.getName(), "priority", priorityName)
                    .register(meterRegistry);
        }

        for (var priority : queues.get(errQueueName).keySet()) {
            String priorityName = Priority.values()[priority].name();

            Gauge.builder("error.queue.size", queues, queues -> queues.get(errQueueName).get(priority).size())
                    .tags("portal", api.getName(), "priority", priorityName)
                    .register(meterRegistry);
        }
    }

    public synchronized Set<Usina> dequeue(Api api, QueueType type, int amount, Priority priority) {
        String queueName = buildName(api, type);
        Set<Usina> usinas = new HashSet<>();
        var queueWithPriority = queues.get(queueName);

        if (priority != null) {
            RSet<Long> queue = queueWithPriority.get(priority.ordinal());
            Set<Long> ids = queue.removeRandom(amount);
            return new HashSet<>(usinaRepository.findAllById(ids));
        }

        for (RSet<Long> queue : queueWithPriority.values()) {
            if (amount - usinas.size() > 0) {
                Set<Long> ids = queue.removeRandom(amount - usinas.size());

                if (!ids.isEmpty())
                    usinas.addAll(usinaRepository.findAllById(ids));
            }
        }

        return usinas;
    }

    public Set<Usina> getUsinasByApi(Api api, QueueType type, int amount, Priority priority) {
        if (type == QueueType.AVAILABLE_OR_FAILED) {
            Set<Usina> usinas = dequeue(api, QueueType.AVAILABLE, amount, priority);
            if (usinas.size() < amount) {
                Set<Usina> failed = dequeue(api, QueueType.FAILED, amount - usinas.size(), priority);
                usinas.addAll(failed);
            }

            return usinas;
        }

        return dequeue(api, type, amount, priority);
    }

    public void queueFailed(List<Usina> usinas, Api api) {
        String errQueueName = buildName(api, QueueType.FAILED);
        var apiQueues = queues.get(errQueueName);

        for (Usina usina : usinas) {
            var queue = apiQueues.get(usina.getPriority().ordinal());
            queue.add(usina.getId());
        }
    }

    public String buildName(Api api, QueueType type) {
        return api.getName() + "_" + type.name();
    }
}
