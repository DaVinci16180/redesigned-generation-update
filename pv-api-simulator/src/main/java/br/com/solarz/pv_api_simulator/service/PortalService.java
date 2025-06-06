package br.com.solarz.pv_api_simulator.service;

import br.com.solarz.pv_api_simulator.model.Portal;
import br.com.solarz.pv_api_simulator.utils.PortalLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class PortalService {

    private Map<Integer, Portal> portals;
    private final Map<Integer, AtomicInteger> rateLimit = new ConcurrentHashMap<>();

    private static final Random randomResponseTime = new Random(1);
    private static final Random randomError = new Random(1);
    private static final Random randomTimeout = new Random(1);

    private static final Random generation = new Random();

    @PostConstruct
    public void populate() {
        portals = PortalLoader.carregarPortais("C:\\Users\\ddaav\\OneDrive\\Documentos\\UFERSA\\TCC 2\\Projeto\\pv-api-simulator\\src\\main\\resources\\static\\portais_simulados.json");
        Executors
                .newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(rateLimit::clear, 0, 1, TimeUnit.MINUTES);
    }

    public int getGeneration(int portalId) throws InterruptedException {
        simulate(portalId);
        return generation.nextInt(1000);
    }

    private void simulate(int portalId) throws InterruptedException {
        Portal portal = portals.get(portalId);
        if (portal == null)
            throw new IllegalArgumentException("Portal não encontrado: " + portalId);

        // Rate Limiting
        if (portal.getRequestsPerMinute() > 0) {
            rateLimit.putIfAbsent(portalId, new AtomicInteger(0));
            int count = rateLimit.get(portalId).incrementAndGet();

            if (count > portal.getRequestsPerMinute())
                throw new RuntimeException("Rate limit excedido para portal " + portal.getName());
        }

        // Simular erro
        if (randomError.nextDouble() < portal.getFailureRate()) {
            throw new RuntimeException("Erro simulado na requisição para o portal " + portal.getName());
        }

        // Simular timeout
        if (randomTimeout.nextDouble() < portal.getTimeoutRate()) {
            Thread.sleep(portal.getTimeoutDuration());
            throw new RuntimeException("Timeout simulado para portal " + portal.getName());
        }

        // Simular tempo normal de resposta
        int min = portal.getMinResponseTime();
        int max = portal.getMaxResponseTime();
        int delay = randomResponseTime.nextInt(max - min) + min;

        Thread.sleep(delay);

        System.out.println("Requisição ao portal " + portal.getName() + " concluída em " + delay + " ms");
    }
}
