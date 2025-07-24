package br.com.solarz.worker.scheduler;

import model.Api;
import model.ApiScore;
import repository.ApiRepository;
import br.com.solarz.worker.service.GenerationUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import repository.ApiScoreRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerationUpdateScheduler {

    public enum RunningSolution {
        ORIGINAL_SOLUTION,
        SOLUTION_1,
        SOLUTION_2,
        SOLUTION_3,
        SOLUTION_4,
        NONE
    }

    private final GenerationUpdateService generationUpdateService;
    private final ApiScoreRepository apiScoreRepository;
    private final ApiRepository apiRepository;

    public static RunningSolution solution = RunningSolution.NONE;

    @Scheduled(cron = "*/5 * * * * *")
    public void processarAtualizacaoDeGeracaoFila() {
        System.out.println(solution.name());

        if (solution.equals(RunningSolution.NONE))
            return;

        switch(solution) {
            case ORIGINAL_SOLUTION: originalSolution(); break;
            case SOLUTION_1: solution1(); break;
            case SOLUTION_2: solution2(); break;
            case SOLUTION_3: solution3(); break;
            case SOLUTION_4: solution4(); break;
        }
    }

    private void originalSolution() {
        List<Api> apis = apiRepository.findAll();

        for (Api api : apis)
            generationUpdateService.updateGenerationByApi(api);
    }

    private void solution1() {
        List<ApiScore> apiScores = apiScoreRepository
                .findAllByPendingGreaterThan(0)
                .stream()
                .sorted()
                .toList();

        for (ApiScore score : apiScores)
            generationUpdateService.updateGenerationByApi(score.getApi());
    }

    private void solution2() {
        for (int i = 0; i < 50; i++)
            generationUpdateService.updateGeneration();
    }

    private void solution3() {
        for (int i = 0; i < 50; i++)
            generationUpdateService.updateGeneration();
    }

    private void solution4() {
        for (int i = 0; i < 50; i++)
            generationUpdateService.updateGeneration();
    }
}
