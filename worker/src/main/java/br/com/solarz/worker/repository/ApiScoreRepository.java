package br.com.solarz.worker.repository;

import br.com.solarz.worker.model.ApiScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiScoreRepository extends JpaRepository<ApiScore, Long> {
}
