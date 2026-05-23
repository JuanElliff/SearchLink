package ar.edu.uade.searchlink.repository;

import ar.edu.uade.searchlink.model.Dispositivo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DispositivoRepository extends MongoRepository<Dispositivo, String> {

    List<Dispositivo> findByUsuarioIdAndActivoTrue(String usuarioId);
}
