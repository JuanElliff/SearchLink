package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.dto.ActualizarAlertaRequest;
import ar.edu.uade.searchlink.dto.CrearAlertaRequest;
import ar.edu.uade.searchlink.exception.RecursoNoEncontradoException;
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

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertaService {

    /** Vigencia por defecto de una alerta; al vencer, el índice TTL la elimina. */
    private static final int DIAS_VIGENCIA_DEFAULT = 30;

    private final AlertaRepository alertaRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Crea una alerta a partir del request del cliente. Los campos server-side se fijan acá,
     * NO se toman del body:
     *  - `creadoPor`: id del operador autenticado (viene del token, parámetro creadoPor).
     *  - `estado`: ACTIVA.
     *  - `creadaEn` / `actualizadaEn`: ahora.
     *  - `expiraEn`: ahora + vigencia por defecto (alimenta el TTL).
     */
    public Alerta crear(CrearAlertaRequest req, String creadoPor) {
        Date ahora = new Date();
        Alerta alerta = Alerta.builder()
                .nombreMenor(req.nombreMenor())
                .edad(req.edad())
                .descripcion(req.descripcion())
                .fotoUrl(req.fotoUrl())
                // GeoJSON usa orden [longitud, latitud] → GeoJsonPoint(x=lng, y=lat).
                .ubicacion(new GeoJsonPoint(req.ubicacion().longitud(), req.ubicacion().latitud()))
                .radioKm(req.radioKm())
                .estado(EstadoAlerta.ACTIVA)
                .creadaPor(creadoPor)
                .creadaEn(ahora)
                .actualizadaEn(ahora)
                .expiraEn(Date.from(ahora.toInstant().plus(DIAS_VIGENCIA_DEFAULT, ChronoUnit.DAYS)))
                .build();

        Alerta guardada = alertaRepository.save(alerta);
        notificarUsuariosCercanos(guardada);
        return guardada;
    }

    public List<Alerta> listarActivas() {
        return alertaRepository.findByEstado(EstadoAlerta.ACTIVA);
    }

    /** Actualización parcial: aplica sólo los campos no nulos del request. */
    public Alerta actualizar(String id, ActualizarAlertaRequest req) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alerta no encontrada: " + id));
        if (req.estado() != null) {
            alerta.setEstado(req.estado());
        }
        if (req.radioKm() != null) {
            alerta.setRadioKm(req.radioKm());
        }
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
