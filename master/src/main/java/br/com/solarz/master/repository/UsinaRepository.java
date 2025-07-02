package br.com.solarz.master.repository;

import br.com.solarz.master.model.Api;
import br.com.solarz.master.model.Credencial;
import br.com.solarz.master.model.Usina;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsinaRepository extends JpaRepository<Usina, Long> {
    List<Usina> findAllByCredencial(Credencial credencial);

    @Query("""
        SELECT u FROM Usina u
        JOIN u.credencial c
        JOIN c.api a
        WHERE a.id = :apiId
        AND u.priority = :priority
    """)
    List<Usina> findAllByCredencial_ApiAndPriority(@Param("apiId") Long apiId, @Param("priority") int priority);
}
