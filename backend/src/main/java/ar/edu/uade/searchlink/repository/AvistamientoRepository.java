package ar.edu.uade.searchlink.repository;

import ar.edu.uade.searchlink.model.Avistamiento;
import ar.edu.uade.searchlink.model.EstadoVerificacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AvistamientoRepository extends MongoRepository<Avistamiento, String> {

    List<Avistamiento> findByAlertaId(String alertaId);

    List<Avistamiento> findByAlertaIdAndEstado(String alertaId, EstadoVerificacion estado);
}
