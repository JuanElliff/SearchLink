package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.model.Alerta;
import ar.edu.uade.searchlink.model.EstadoAlerta;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final MongoTemplate mongoTemplate;

    public Alerta crear(Alerta alerta) {
        alerta.setEstado(EstadoAlerta.ACTIVA);
        alerta.setCreadaEn(new Date());
        alerta.setActualizadaEn(new Date());
        Alerta guardada = alertaRepository.save(alerta);
        notificarUsuariosCercanos(guardada);
        return guardada;
    }

    public List<Alerta> listarActivas() {
        return alertaRepository.findByEstado(EstadoAlerta.ACTIVA);
    }

    public Alerta cambiarEstado(String id, EstadoAlerta nuevoEstado) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada: " + id));
        alerta.setEstado(nuevoEstado);
        alerta.setActualizadaEn(new Date());
        return alertaRepository.save(alerta);
    }

    private void notificarUsuariosCercanos(Alerta alerta) {
        Point centro = new Point(alerta.getUbicacion().getX(), alerta.getUbicacion().getY());
        double radioKm = alerta.getRadioKm() != null ? alerta.getRadioKm() : 10.0;

        NearQuery nearQuery = NearQuery.near(centro)
                .maxDistance(new Distance(radioKm, Metrics.KILOMETERS))
                .spherical(true)
                .query(Query.query(Criteria.where("activo").is(true)));

        List<Usuario> usuariosCercanos = mongoTemplate
                .geoNear(nearQuery, Usuario.class)
                .getContent()
                .stream()
                .map(GeoResult::getContent)
                .collect(Collectors.toList());

        // TODO: despachar notificaciones FCM a usuariosCercanos
    }
}
