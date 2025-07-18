package br.com.solarz.master.service;

import br.com.solarz.master.MasterApplication;
import br.com.solarz.master.helpers.PopulateDatabaseHelper;
import br.com.solarz.master.scheduler.MetricsScheduler;
import config.RedisClientProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import model.Api;
import model.Credencial;
import model.Usina;
import model.Usina.Priority;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import repository.ApiRepository;
import repository.ApiScoreRepository;
import repository.CredencialRepository;
import repository.UsinaRepository;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueueBuilderService_Original {

    private final ApiScoreRepository apiScoreRepository;

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
    private final RestClient client = RestClient.create();
    Logger logger = LoggerFactory.getLogger(QueueBuilderService_Original.class);

    private final PopulateDatabaseHelper populate;
    private final CredencialRepository credencialRepository;
    private final RedisClientProvider redisClientProvider;
    private final UsinaRepository usinaRepository;
    private final ApiRepository apiRepository;

    @PostConstruct
    public void setup() {
        this.redissonClient = redisClientProvider.getClient();

        populate.populateDatabase();

        setupQueues();
        buildQueues();
        startSim();
    }

    public void startSim() {
        Map<String, String> body = new HashMap<>();
        body.put("operation", "start");

        for (String addr : MasterApplication.WORKERS_ADDR) {
            client.post()
                    .uri("http://" + addr + ":8081/simulation/change-state")
                    .body(body)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        }

        if (MetricsScheduler.start == null)
            MetricsScheduler.start = Instant.now();

        logger.info("Simulation started");

        try {
            FileOutputStream fos = new FileOutputStream("logs.txt", true);
            fos.write(("\n" + ZonedDateTime.now() + " - Simulation started\n").getBytes());
            fos.close();
        } catch (IOException ignored) {}
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
                usinas = usinas.stream().peek(Usina::reset).toList();
                usinaRepository.saveAll(usinas);

                List<Usina> usinasHigh = usinas.stream().filter(u -> u.getPriority().equals(Priority.HIGH)).toList();
                List<Usina> usinasNorm = usinas.stream().filter(u -> u.getPriority().equals(Priority.NORMAL)).toList();

                var queueHigh = queues.get(avaQueueName).get(Priority.HIGH.ordinal());
                var queueNorm = queues.get(avaQueueName).get(Priority.NORMAL.ordinal());

                queueHigh.addAll(usinasHigh.stream().map(Usina::getId).toList());
                queueNorm.addAll(usinasNorm.stream().map(Usina::getId).toList());
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
