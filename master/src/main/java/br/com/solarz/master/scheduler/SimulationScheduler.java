package br.com.solarz.master.scheduler;

import br.com.solarz.master.service.SolutionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import repository.UsinaRepository;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationScheduler {

    Logger logger = LoggerFactory.getLogger(SimulationScheduler.class);

    private final UsinaRepository usinaRepository;
    private final SolutionService solutionService;

    private boolean settingUp = false;
    public static Instant start = null;
    private int checkpoint = 1; // minutos
    Deque<Runnable> solutions = new ArrayDeque<>();

    @PostConstruct
    public void init() {
        solutions.push(solutionService.originalSolution());
        solutions.push(solutionService.solution1());
        solutions.push(solutionService.solution2());
        solutions.push(solutionService.solution3());
        solutions.push(solutionService.solution4());
    }

    @Scheduled(cron = "*/5 * * * * *")
    public void simulate() throws IOException {
        if (settingUp)
            return;

        if (start == null) {
            settingUp = true;
            System.out.println("Iniciando setup da próxima solução pega da fila.");

            Runnable solution = solutions.poll();
            if (solution == null)
                return;

            solution.run();

            System.out.println("Setup concluido.");
            settingUp = false;
        }

        Instant now = Instant.now();
        if (Duration.between(start, now).toMinutes() >= checkpoint) {
            int updated = usinaRepository.countByUpdated(true);
            int notUpdated = usinaRepository.countByUpdated(false);
            logger.info("Usinas atualizadas em {} minutos: {}", checkpoint, updated);

            FileOutputStream fos = new FileOutputStream("logs.txt", true);
            fos.write((ZonedDateTime.now() + " - Usinas atualizadas em " + checkpoint + " minutos: " + updated + "\n").getBytes());
            fos.close();

            checkpoint += 1;

            if (notUpdated == 0) {
                start = null;
                checkpoint = 1;
            }
        }
    }
}
