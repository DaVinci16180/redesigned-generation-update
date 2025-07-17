package br.com.solarz.worker.repository;

import br.com.solarz.worker.model.ApiScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiScoreRepository extends JpaRepository<ApiScore, Long> {
    List<ApiScore> findAllByPendingGreaterThan(double pendingIsGreaterThan);
}
