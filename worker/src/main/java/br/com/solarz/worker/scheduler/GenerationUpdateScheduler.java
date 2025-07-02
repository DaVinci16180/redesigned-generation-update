package br.com.solarz.worker.scheduler;

import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.repository.ApiRepository;
import br.com.solarz.worker.service.GenerationUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerationUpdateScheduler {

    private final GenerationUpdateService generationUpdateService;
    private final ApiRepository apiRepository;

    public static boolean RUNNING = false;

    @Scheduled(cron = "*/10 * * * * *")
    public void processarAtualizacaoDeGeracaoFila() {
        if (!RUNNING) {
            System.out.println("Skipping generationUpdateScheduler");
            return;
        }

        List<Api> apis = apiRepository.findAll();
        for (var api : apis)
            generationUpdateService.updateGenerationByApi(api);
    }
}
