package br.com.solarz.worker.scheduler;

import br.com.solarz.worker.model.ApiScore;
import br.com.solarz.worker.repository.ApiRepository;
import br.com.solarz.worker.repository.ApiScoreRepository;
import br.com.solarz.worker.service.GenerationUpdateInterface;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GenerationUpdateScheduler {

    @Qualifier("original")
    private final GenerationUpdateInterface generationUpdateService;
    private final ApiScoreRepository apiScoreRepository;
    private final ApiRepository apiRepository;

    public static boolean RUNNING = false;

    @PostConstruct
    public void setup() {

    }

    @Scheduled(cron = "*/10 * * * * *")
    public void processarAtualizacaoDeGeracaoFila() {
        if (!RUNNING)
            return;

        List<ApiScore> apiScores = apiScoreRepository.findAll();
        apiScores.sort(null);

        for (ApiScore score : apiScores)
            generationUpdateService.updateGenerationByApi(score.getApi(), score);
    }
}
