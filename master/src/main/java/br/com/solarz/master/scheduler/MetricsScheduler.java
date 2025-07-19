package br.com.solarz.master.scheduler;

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

@Service
@RequiredArgsConstructor
public class MetricsScheduler {

    Logger logger = LoggerFactory.getLogger(MetricsScheduler.class);

    private final UsinaRepository usinaRepository;

    public static Instant start = null;
    private int checkpoint = 2; // minutos

    @Scheduled(cron = "*/5 * * * * *")
    public void processarAtualizacaoDeGeracaoFila() throws IOException {
        if (start == null)
            return;

        Instant now = Instant.now();
        if (Duration.between(start, now).toMinutes() >= checkpoint) {
            int updated = usinaRepository.countByUpdated(true);
            logger.info("Usinas atualizadas em {} minutos: {}", checkpoint, updated);

            FileOutputStream fos = new FileOutputStream("logs.txt", true);
            fos.write((ZonedDateTime.now() + " - Usinas atualizadas em " + checkpoint + " minutos: " + updated + "\n").getBytes());
            fos.close();

            checkpoint += 2;
        }
    }
}
