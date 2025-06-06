package br.com.solarz.master.repository;

import br.com.solarz.master.model.Api;
import br.com.solarz.master.model.Credencial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CredencialRepository extends JpaRepository<Credencial, Long> {
    List<Credencial> findAllByApi(Api api);
}
