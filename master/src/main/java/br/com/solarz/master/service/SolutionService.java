package br.com.solarz.master.service;

import br.com.solarz.master.MasterApplication;
import br.com.solarz.master.scheduler.MetricsScheduler;
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
public class SolutionService implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(SolutionService.class);
    private final RestClient client = RestClient.create();

    private final CompositeQueueService compositeQueueService;

    @Override
    public void run() {
        originalSolution();
    }

    public void originalSolution() {
        compositeQueueService.setupQueues();
        compositeQueueService.buildQueues();
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

        if (MetricsScheduler.start == null)
            MetricsScheduler.start = Instant.now();

        logger.info("Simulation stopped");

        try {
            FileOutputStream fos = new FileOutputStream("logs.txt", true);
            fos.write((ZonedDateTime.now() + " - Simulation started\n").getBytes());
            fos.close();
        } catch (IOException ignored) {}
    }
}
