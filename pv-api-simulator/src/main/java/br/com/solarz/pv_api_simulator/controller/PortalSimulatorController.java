package br.com.solarz.pv_api_simulator.controller;

import br.com.solarz.pv_api_simulator.service.PortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/portal")
public class PortalSimulatorController {

    private final PortalService portalService;

    @GetMapping("/generation")
    public ResponseEntity<Integer> generation(
            @RequestParam int portalId
    ) throws InterruptedException {
        int result = portalService.getGeneration(portalId);
        return ResponseEntity.ok(result);
    }
}
