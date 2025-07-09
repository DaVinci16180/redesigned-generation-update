package br.com.solarz.worker.repository;

import br.com.solarz.worker.model.Api;
import br.com.solarz.worker.model.Usina;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface UsinaRepository extends JpaRepository<Usina, Long> {
    @Query("""
        SELECT count(u) FROM Usina u
        JOIN u.credencial c
        JOIN c.api a
        WHERE a.id = :apiId
    """)
    int countByApiId(@Param("apiId") Long apiId);

    @Query("""
        SELECT count(u) FROM Usina u
        JOIN u.credencial c
        JOIN c.api a
        WHERE a.id = :apiId
        AND u.updated = false
    """)
    int countNotUpdatedByApiId(@Param("apiId") Long apiId);
}
