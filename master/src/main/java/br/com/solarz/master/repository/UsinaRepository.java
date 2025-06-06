package br.com.solarz.master.repository;

import br.com.solarz.master.model.Credencial;
import br.com.solarz.master.model.Usina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsinaRepository extends JpaRepository<Usina, Long> {
    List<Usina> findAllByCredencial(Credencial credencial);
}
