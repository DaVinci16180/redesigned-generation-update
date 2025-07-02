package br.com.solarz.worker.controller;

import br.com.solarz.worker.scheduler.GenerationUpdateScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/simulation")
public class RunSimulationController {

    @PostMapping("/change-state")
    public ResponseEntity<?> controlRunningState(@RequestBody Map<String, String> params) {
        String operation = params.get("operation");

        if (operation.equals("start")) {
            GenerationUpdateScheduler.RUNNING = true;
            return ResponseEntity.ok("Simulação iniciada");
        } else if (operation.equals("stop")) {
            GenerationUpdateScheduler.RUNNING = false;
            return ResponseEntity.ok("Simulação interrompida");
        }

        return ResponseEntity.badRequest().build();
    }
}
