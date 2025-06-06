package br.com.solarz.worker.repository;

import br.com.solarz.worker.model.Usina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsinaRepository extends JpaRepository<Usina, Long> {
}
