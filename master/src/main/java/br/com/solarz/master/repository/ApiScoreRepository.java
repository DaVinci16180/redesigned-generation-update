package br.com.solarz.master.repository;

import br.com.solarz.master.model.ApiScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiScoreRepository extends JpaRepository<ApiScore, Long> {
}
