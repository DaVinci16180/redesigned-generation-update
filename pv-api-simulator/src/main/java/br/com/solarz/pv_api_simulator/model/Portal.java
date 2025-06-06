package br.com.solarz.pv_api_simulator.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Portal {
    private int id;
    private String name;

    // Tempo de resposta
    private int minResponseTime;
    private int maxResponseTime;

    // Limite de requisições por minuto (rate limit)
    private int requestsPerMinute = -1;

    // Percentual de falhas simuladas (0.0 a 1.0)
    private double failureRate = 0;

    // Percentual de timeouts simulados (0.0 a 1.0)
    private double timeoutRate = 0;
    private int timeoutDuration = 120;
}
