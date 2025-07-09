package br.com.solarz.worker;

import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.model.ApiScore;
import br.com.solarz.worker.repository.ApiRepository;
import br.com.solarz.worker.service.GenerationUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GenerationUpdateTests {

    @Autowired
    private GenerationUpdateService generationUpdateService;
    @Autowired
    private ApiRepository apiRepository;

    @Test
    void updateGeneration() {
        Api api = apiRepository.findById(1L).orElseThrow();
        generationUpdateService.updateGenerationByApi(api, new ApiScore(api));
    }
}
