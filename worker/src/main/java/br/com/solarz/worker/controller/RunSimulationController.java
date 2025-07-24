package br.com.solarz.worker.controller;

import br.com.solarz.worker.scheduler.GenerationUpdateScheduler;
import br.com.solarz.worker.scheduler.GenerationUpdateScheduler.*;
import br.com.solarz.worker.service.SingleQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/simulation")
@RequiredArgsConstructor
public class RunSimulationController {

    private final SingleQueueService singleQueueService;
    @Qualifier("generationUpdate")
    private final ThreadPoolTaskExecutor executor;

    @PostMapping("/change-state")
    public ResponseEntity<?> controlRunningState(@RequestBody Map<String, String> params) {
        String operation = params.get("operation");

        switch (operation) {
            case "original" -> {
                GenerationUpdateScheduler.solution = RunningSolution.ORIGINAL_SOLUTION;
                return ResponseEntity.ok("Simulação iniciada");
            }
            case "1" -> {
                GenerationUpdateScheduler.solution = RunningSolution.SOLUTION_1;
                return ResponseEntity.ok("Simulação iniciada");
            }
            case "2" -> {
                GenerationUpdateScheduler.solution = RunningSolution.SOLUTION_2;
                singleQueueService.buildMeters();
                return ResponseEntity.ok("Simulação iniciada");
            }
            case "3" -> {
                GenerationUpdateScheduler.solution = RunningSolution.SOLUTION_3;
                singleQueueService.buildMeters();
                return ResponseEntity.ok("Simulação iniciada");
            }
            case "4" -> {
                GenerationUpdateScheduler.solution = RunningSolution.SOLUTION_4;
                singleQueueService.buildMeters();
                return ResponseEntity.ok("Simulação iniciada");
            }
            case "stop" -> {
                GenerationUpdateScheduler.solution = RunningSolution.NONE;
                executor.shutdown();
                executor.initialize();
                return ResponseEntity.ok("Simulação interrompida");
            }
        }

        return ResponseEntity.badRequest().build();
    }
}
