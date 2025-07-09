package br.com.solarz.worker.service;

import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.model.ApiScore;

public interface GenerationUpdateInterface {
    void updateGenerationByApi(Api api, ApiScore score);
}
