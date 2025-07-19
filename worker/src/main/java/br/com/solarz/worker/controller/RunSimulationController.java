package br.com.solarz.worker.controller;

import br.com.solarz.worker.scheduler.GenerationUpdateScheduler;
import br.com.solarz.worker.service.firstsolution.GenerationUpdateService_FirstSolution;
import util.ApiAverages;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @GetMapping("/averages")
    public ResponseEntity<String> averages() throws JsonProcessingException {
        var averages = GenerationUpdateService_FirstSolution.averages;

        Map<Long, ApiAverages> result = new HashMap<>();
        for (var avg : averages.entrySet())
            result.put(avg.getKey().getId(), avg.getValue());

        ObjectMapper objectMapper = new ObjectMapper();
        return ResponseEntity.ok(objectMapper.writeValueAsString(result));
    }
}
