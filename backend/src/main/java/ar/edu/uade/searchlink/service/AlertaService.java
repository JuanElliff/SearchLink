package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.dto.ActualizarAlertaRequest;
import ar.edu.uade.searchlink.dto.CrearAlertaRequest;
import ar.edu.uade.searchlink.exception.RecursoNoEncontradoException;
import ar.edu.uade.searchlink.model.Alerta;
import ar.edu.uade.searchlink.model.EstadoAlerta;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);

    /** Vigencia por defecto de una alerta; al vencer, el índice TTL la elimina. */
    private static final int DIAS_VIGENCIA_DEFAULT = 30;

    private final AlertaRepository alertaRepository;
    private final MongoTemplate mongoTemplate;
    private final FcmService fcmService;

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

    /**
     * Detalle de una alerta por id, sin importar su estado (activa/resuelta/cancelada): el
     * deep-link del push debe poder abrir la alerta aunque ya no esté activa. 404 si no existe.
     */
    public Alerta obtenerPorId(String id) {
        return alertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alerta no encontrada: " + id));
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

        // Dispatch FCM + depuración de tokens muertos. Envuelto para que NINGUNA excepción se
        // propague a crear(): la alerta ya fue guardada y el POST debe responder OK aunque el push
        // falle. FcmService.enviarAlerta ya es no-throw; el $pull podría fallar y se aísla acá.
        try {
            List<String> muertos = fcmService.enviarAlerta(tokensActivos, alerta);
            depurarTokensMuertos(muertos);
        } catch (Exception e) {
            log.error("Fallo en dispatch/depuración FCM para la alerta {}; la alerta ya fue creada",
                    alerta.getId(), e);
        }
    }

    /**
     * Depura los tokens FCM muertos (UNREGISTERED) de TODOS los usuarios en una sola operación:
     * $pull de los sub-documentos de `dispositivos` cuyo `fcm_token` esté en la lista.
     */
    private void depurarTokensMuertos(List<String> muertos) {
        if (muertos == null || muertos.isEmpty()) {
            return;
        }
        Query filtro = new Query(Criteria.where("dispositivos.fcm_token").in(muertos));
        Update pull = new Update().pull("dispositivos",
                new Document("fcm_token", new Document("$in", muertos)));
        mongoTemplate.updateMulti(filtro, pull, Usuario.class);
    }
}
