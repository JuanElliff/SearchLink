package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.dto.CambiarEstadoAvistamientoRequest;
import ar.edu.uade.searchlink.dto.CrearAvistamientoRequest;
import ar.edu.uade.searchlink.exception.RecursoNoEncontradoException;
import ar.edu.uade.searchlink.model.Avistamiento;
import ar.edu.uade.searchlink.model.EstadoVerificacion;
import ar.edu.uade.searchlink.repository.AlertaRepository;
import ar.edu.uade.searchlink.repository.AvistamientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvistamientoService {

    private final AvistamientoRepository avistamientoRepository;
    private final AlertaRepository alertaRepository;

    /**
     * Reporta un avistamiento. Los campos server-side se fijan acá, NO se toman del body:
     *  - `reportadoPor`: id del usuario autenticado (viene del token, parámetro reportadoPor).
     *  - `estado`: PENDIENTE (estado inicial de moderación).
     *  - `creadoEn`: ahora.
     * Valida que la alerta referenciada exista; si no, 404.
     */
    public Avistamiento crear(CrearAvistamientoRequest req, String reportadoPor) {
        if (!alertaRepository.existsById(req.alertaId())) {
            throw new RecursoNoEncontradoException("Alerta no encontrada: " + req.alertaId());
        }
        Avistamiento avistamiento = Avistamiento.builder()
                .alertaId(req.alertaId())
                .reportadoPor(reportadoPor)
                // GeoJSON usa orden [longitud, latitud] → GeoJsonPoint(x=lng, y=lat).
                .ubicacion(new GeoJsonPoint(req.ubicacion().longitud(), req.ubicacion().latitud()))
                .descripcion(req.descripcion())
                .fotoUrl(req.fotoUrl())
                .estado(EstadoVerificacion.PENDIENTE)
                .creadoEn(new Date())
                .build();
        return avistamientoRepository.save(avistamiento);
    }

    public List<Avistamiento> listarPorAlerta(String alertaId) {
        return avistamientoRepository.findByAlertaId(alertaId);
    }

    public Avistamiento obtener(String id) {
        return avistamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Avistamiento no encontrado: " + id));
    }

    /**
     * Moderación: un OPERADOR transiciona el estado a VERIFICADO o DESCARTADO. El destino ya
     * viene validado en el request (no puede ser PENDIENTE). El comentario es opcional: sólo se
     * aplica si viene no nulo.
     */
    public Avistamiento cambiarEstado(String id, CambiarEstadoAvistamientoRequest req) {
        Avistamiento avistamiento = obtener(id);
        avistamiento.setEstado(req.nuevoEstado());
        if (req.comentariosAdmin() != null) {
            avistamiento.setComentariosAdmin(req.comentariosAdmin());
        }
        return avistamientoRepository.save(avistamiento);
    }
}
