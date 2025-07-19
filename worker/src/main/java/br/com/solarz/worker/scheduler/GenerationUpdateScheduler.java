package br.com.solarz.worker.scheduler;

import br.com.solarz.worker.service.GenerationUpdateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerationUpdateScheduler {

    private final GenerationUpdateService generationUpdateService;

    public static boolean RUNNING = false;

    @PostConstruct
    public void setup() {

    }

    @Scheduled(cron = "*/5 * * * * *")
    public void processarAtualizacaoDeGeracaoFila() {
        if (!RUNNING)
            return;

        for (int i = 0; i < 50; i++)
            generationUpdateService.updateGeneration();
    }
}
