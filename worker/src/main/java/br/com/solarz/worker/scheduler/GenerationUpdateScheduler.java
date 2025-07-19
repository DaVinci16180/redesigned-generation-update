package br.com.solarz.worker.scheduler;

import model.ApiScore;
import repository.ApiScoreRepository;
import br.com.solarz.worker.service.GenerationUpdateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerationUpdateScheduler {

    private final GenerationUpdateService firstSolution;
    private final ApiScoreRepository apiScoreRepository;

    public static boolean RUNNING = false;

    @PostConstruct
    public void setup() {

    }

    @Scheduled(cron = "*/5 * * * * *")
    public void processarAtualizacaoDeGeracaoFila() {
        if (!RUNNING)
            return;

        List<ApiScore> apiScores = apiScoreRepository
                .findAllByPendingGreaterThan(0)
                .stream()
                .sorted()
                .toList();

        for (ApiScore score : apiScores)
            firstSolution.updateGenerationByApi(score.getApi());
    }
}
