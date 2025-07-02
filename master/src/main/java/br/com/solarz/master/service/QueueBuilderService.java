package br.com.solarz.master.service;

import br.com.solarz.master.config.RedisClientProvider;
import br.com.solarz.master.helpers.PopulateDatabaseHelper;
import br.com.solarz.master.model.Api;
import br.com.solarz.master.model.Credencial;
import br.com.solarz.master.model.Usina;
import br.com.solarz.master.model.Usina.Priority;
import br.com.solarz.master.repository.ApiRepository;
import br.com.solarz.master.repository.CredencialRepository;
import br.com.solarz.master.repository.UsinaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueBuilderService {

    public enum QueueType {
        AVAILABLE, // fila usinas disponíveis para atualização
        FAILED, // fila usinas que falharam a atualização
        AVAILABLE_OR_FAILED //usado para pegar dados das duas filas ao mesmo tempo, priorizando a fila available
    };

    /*
     * PortalName: {
     *     HIGH: [...ids],
     *     NORMAL: [...ids]
     * }
     */
    private final HashMap<String, HashMap<Integer, RSet<Long>>> queues = new HashMap<>();
    private RedissonClient redissonClient;

    private final PopulateDatabaseHelper populate;
    private final CredencialRepository credencialRepository;
    private final RedisClientProvider redisClientProvider;
    private final UsinaRepository usinaRepository;
    private final ApiRepository apiRepository;

    @PostConstruct
    public void setup() {
        this.redissonClient = redisClientProvider.getClient();

        populate.populateApis();
        populate.populateCredenciais();
        populate.populateUsinas();

        setupQueues();
        buildQueues();
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

    public void buildQueues() {
        Instant start = Instant.now();
        List<Api> apis = apiRepository.findAll();
        clearQueues(apis);

        for (Api api : apis) {
            String avaQueueName = buildName(api, QueueType.AVAILABLE);
            List<Credencial> credenciais = credencialRepository.findAllByApi(api);

            for (Credencial credencial : credenciais) {
                List<Usina> usinas = usinaRepository.findAllByCredencial(credencial);

                List<Usina> usinasHigh = usinas.stream().filter(u -> u.getPriority().equals(Priority.HIGH)).toList();
                List<Usina> usinasNorm = usinas.stream().filter(u -> u.getPriority().equals(Priority.NORMAL)).toList();

                var queueHigh = queues.get(avaQueueName).get(Priority.HIGH.ordinal());
                var queueNorm = queues.get(avaQueueName).get(Priority.NORMAL.ordinal());

                queueHigh.addAll(usinasHigh.stream().map(Usina::getId).toList());
                queueNorm.addAll(usinasNorm.stream().map(Usina::getId).toList());

//                for (Usina usina : usinas) {
//                    var queue = queues.get(avaQueueName).get(usina.getPriority().ordinal());
//                    queue.add(usina.getId());
//                }
            }
        }

        Instant finish = Instant.now();
        Duration duration = Duration.between(start, finish);

        System.out.println("Building queues took " + duration.toSeconds() + " seconds");
    }

    public void clearQueues(List<Api> apis) {
        for (Api api : apis) {
            String avaQueueName = buildName(api, QueueType.AVAILABLE);
            String errQueueName = buildName(api, QueueType.FAILED);

            if (queues.containsKey(avaQueueName))
                for (var queue : queues.get(avaQueueName).values())
                    queue.clear();

            if (queues.containsKey(errQueueName))
                for (var queue : queues.get(errQueueName).values())
                    queue.clear();
        }
    }

    public String buildName(Api api, QueueType type) {
        return api.getName() + "_" + type.name();
    }
}
