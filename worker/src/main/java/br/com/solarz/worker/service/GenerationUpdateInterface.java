package br.com.solarz.worker.service;

import model.Api;
import model.ApiScore;

public interface GenerationUpdateInterface {
    void updateGenerationByApi(Api api, ApiScore score);
}
