package br.com.solarz.master.controller;

import br.com.solarz.master.MasterApplication;
import br.com.solarz.master.scheduler.MetricsScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MasterController {

    Logger logger = LoggerFactory.getLogger(MasterController.class);
    private final RestClient client = RestClient.create();

    @GetMapping("/simulation/start")
    public ResponseEntity<?> startSimulation() {
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

        logger.info("Simulation started");
        if (MetricsScheduler.start == null)
            MetricsScheduler.start = Instant.now();

        return ResponseEntity.ok("Simulação iniciada.");
    }

    @GetMapping("/simulation/stop")
    public ResponseEntity<?> stopSimulation() {
        Map<String, String> body = new HashMap<>();
        body.put("operation", "stop");

        for (String addr : MasterApplication.WORKERS_ADDR) {
            String result = client.post()
                    .uri("http://" + addr + ":8081/simulation/change-state")
                    .body(body)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            System.out.println(result);
        }

        return ResponseEntity.ok().build();
    }
}
