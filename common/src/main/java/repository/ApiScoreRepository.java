package repository;

import model.ApiScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiScoreRepository extends JpaRepository<ApiScore, Long> {

    List<ApiScore> findAllByPendingGreaterThan(double pendingIsGreaterThan);
}
