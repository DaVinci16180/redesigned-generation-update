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

        if (operation.equals("original")) {
            GenerationUpdateScheduler.solution = RunningSolution.ORIGINAL_SOLUTION;
            return ResponseEntity.ok("Simulação iniciada");
        } if (operation.equals("1")) {
            GenerationUpdateScheduler.solution = RunningSolution.SOLUTION_1;
            return ResponseEntity.ok("Simulação iniciada");
        } if (operation.equals("2")) {
            GenerationUpdateScheduler.solution = RunningSolution.SOLUTION_2;
            return ResponseEntity.ok("Simulação iniciada");
        } if (operation.equals("3")) {
            GenerationUpdateScheduler.solution = RunningSolution.SOLUTION_3;
            return ResponseEntity.ok("Simulação iniciada");
        } if (operation.equals("4")) {
            GenerationUpdateScheduler.solution = RunningSolution.SOLUTION_4;
            return ResponseEntity.ok("Simulação iniciada");
        } else if (operation.equals("stop")) {
            GenerationUpdateScheduler.solution = null;
            return ResponseEntity.ok("Simulação interrompida");
        }

        return ResponseEntity.badRequest().build();
    }
}
