package ar.edu.uade.searchlink.repository;

import ar.edu.uade.searchlink.model.Alerta;
import ar.edu.uade.searchlink.model.EstadoAlerta;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AlertaRepository extends MongoRepository<Alerta, String> {

    List<Alerta> findByEstado(EstadoAlerta estado);
}
