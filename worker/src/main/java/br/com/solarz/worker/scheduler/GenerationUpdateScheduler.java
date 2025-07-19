package br.com.solarz.worker.scheduler;

import model.Api;
import repository.ApiRepository;
import br.com.solarz.worker.service.GenerationUpdateService;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void setup() {

    }

    @Scheduled(cron = "*/5 * * * * *")
    public void processarAtualizacaoDeGeracaoFila() {
        if (!RUNNING)
            return;

        List<Api> apis = apiRepository.findAll();

        for (Api api : apis)
            generationUpdateService.updateGenerationByApi(api);
    }
}
