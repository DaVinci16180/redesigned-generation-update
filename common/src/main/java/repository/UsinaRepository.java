package repository;

import model.Api;
import model.Credencial;
import model.Usina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsinaRepository extends JpaRepository<Usina, Long> {

    int countByCredencial_Api(Api credencialApi);

    List<Usina> findAllByCredencial(Credencial credencial);

    int countByUpdated(boolean updated);

    int countByUpdatedAndCredencial_Api(boolean updated, Api credencialApi);
}
