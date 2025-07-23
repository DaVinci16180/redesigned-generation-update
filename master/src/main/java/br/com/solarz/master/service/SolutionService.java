package br.com.solarz.master.service;

import br.com.solarz.master.MasterApplication;
import br.com.solarz.master.scheduler.SimulationScheduler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SolutionService {

    private final Logger logger = LoggerFactory.getLogger(SolutionService.class);
    private final RestClient client = RestClient.create();

    private final CompositeQueueService compositeQueueService;
    private final SingleQueueService singleQueueService;

    public Runnable originalSolution() {
        return () -> {
            compositeQueueService.setupQueues();
            compositeQueueService.buildQueues();
            startSim("original");
        };
    }

    public Runnable solution1() {
        return () -> {
            compositeQueueService.setupQueues();
            compositeQueueService.buildQueues();
            startSim("1");
        };
    }

    public Runnable solution2() {
        return () -> {
            singleQueueService.buildQueues(0, 1);
            startSim("2");
        };
    }

    public Runnable solution3() {
        return () -> {
            singleQueueService.buildQueues(0, 1);
            startSim("3");
        };
    }

    public Runnable solution4() {
        return () -> {
            singleQueueService.buildQueues(0, 2);
            startSim("4");
        };
    }

    public void startSim(String solution) {
        Map<String, String> body = new HashMap<>();
        body.put("operation", solution);

        for (String addr : MasterApplication.WORKERS_ADDR) {
            client.post()
                    .uri("http://" + addr + ":8081/simulation/change-state")
                    .body(body)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        }

        if (SimulationScheduler.start == null)
            SimulationScheduler.start = Instant.now();

        logger.info("Solução {} iniciada.", solution);

        try {
            FileOutputStream fos = new FileOutputStream("logs.txt", true);
            fos.write(("\n" + ZonedDateTime.now() + " - Solução " + solution + " iniciada.\n").getBytes());
            fos.close();
        } catch (IOException ignored) {}
    }

    public void stopSim() {
        Map<String, String> body = new HashMap<>();
        body.put("operation", "stop");

        for (String addr : MasterApplication.WORKERS_ADDR) {
            client.post()
                    .uri("http://" + addr + ":8081/simulation/change-state")
                    .body(body)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        }

        if (SimulationScheduler.start == null)
            SimulationScheduler.start = Instant.now();

        logger.info("Simulation stopped");

        try {
            FileOutputStream fos = new FileOutputStream("logs.txt", true);
            fos.write((ZonedDateTime.now() + " - Simulation started\n").getBytes());
            fos.close();
        } catch (IOException ignored) {}
    }
}
