package br.com.solarz.worker.scheduler;

import model.Api;
import model.ApiScore;
import repository.ApiRepository;
import repository.ApiScoreRepository;
import br.com.solarz.worker.service.GenerationUpdateService_FirstSolution;
import br.com.solarz.worker.service.GenerationUpdateService_Original;
import br.com.solarz.worker.service.GenerationUpdateService_SecondSolution;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerationUpdateScheduler {

    private final GenerationUpdateService_SecondSolution secondSolution;
    private final GenerationUpdateService_FirstSolution firstSolution;
    private final GenerationUpdateService_Original original;
    private final ApiScoreRepository apiScoreRepository;
    private final ApiRepository apiRepository;

    public static boolean RUNNING = false;

    @PostConstruct
    public void setup() {

    }

    @Scheduled(cron = "*/5 * * * * *")
    public void processarAtualizacaoDeGeracaoFila() {
        if (!RUNNING)
            return;

//        original();
//        firstSolution();
        secondSolution();
    }

    private void original() {
        List<Api> apis = apiRepository.findAll();

        for (Api api : apis)
            original.updateGenerationByApi(api);
    }

    private void firstSolution() {
        List<ApiScore> apiScores = apiScoreRepository.findAllByPendingGreaterThan(0);
        apiScores.sort(null);

        for (ApiScore score : apiScores)
            firstSolution.updateGenerationByApi(score.getApi());
    }

    private boolean looping = false;
    private void secondSolution() {
//        if (looping)
//            return;
//
//        looping = true;
//
//        while (RUNNING)
        for (int i = 0; i < 50; i++)
            secondSolution.updateGeneration();

//        looping = false;
    }
}
