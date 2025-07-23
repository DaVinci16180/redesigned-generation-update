package br.com.solarz.worker.controller;

import br.com.solarz.worker.scheduler.GenerationUpdateScheduler;
import br.com.solarz.worker.scheduler.GenerationUpdateScheduler.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/simulation")
public class RunSimulationController {

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
                return ResponseEntity.ok("Simulação iniciada");
            }
            case "3" -> {
                GenerationUpdateScheduler.solution = RunningSolution.SOLUTION_3;
                return ResponseEntity.ok("Simulação iniciada");
            }
            case "4" -> {
                GenerationUpdateScheduler.solution = RunningSolution.SOLUTION_4;
                return ResponseEntity.ok("Simulação iniciada");
            }
            case "stop" -> {
                GenerationUpdateScheduler.solution = null;
                return ResponseEntity.ok("Simulação interrompida");
            }
        }

        return ResponseEntity.badRequest().build();
    }
}
