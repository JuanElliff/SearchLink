package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.model.Alerta;
import ar.edu.uade.searchlink.model.EstadoAlerta;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
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
        GeoJsonPoint centro = alerta.getUbicacion();
        double radioKm = alerta.getRadioKm() != null ? alerta.getRadioKm() : 10.0;
        double radioMetros = radioKm * 1000.0;

        // Esta fase consulta solo ubicacion_precargada. La combinacion con
        // ubicacion_actual (coalesce por-usuario) queda para el bloque de geolocalizacion.
        Query query = Query.query(
                Criteria.where("activo").is(true)
                        .and("ubicacion_precargada").nearSphere(centro).maxDistance(radioMetros)
        );

        List<Usuario> usuariosCercanos = mongoTemplate.find(query, Usuario.class);

        List<String> tokensActivos = usuariosCercanos.stream()
                .flatMap(u -> u.getDispositivos() == null
                        ? java.util.stream.Stream.empty()
                        : u.getDispositivos().stream())
                .filter(Usuario.Dispositivo::isActivo)
                .map(Usuario.Dispositivo::getFcmToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());

        // TODO: despachar notificaciones FCM a tokensActivos (Paso 3: Firebase Admin SDK)
    }
}
