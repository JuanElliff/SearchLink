package ar.edu.uade.searchlink.dto;

import ar.edu.uade.searchlink.model.Avistamiento;
import ar.edu.uade.searchlink.model.EstadoVerificacion;

import java.util.Date;
import java.util.List;

/**
 * DTO de salida de avistamiento. No expone la entidad cruda. La ubicación se devuelve como
 * GeoJSON estándar {type, coordinates:[lng,lat]}, igual que AlertaResponse. Se incluye
 * `reportadoPor` (id del usuario que reportó): dato fijado por el servidor, no sensible.
 */
public record AvistamientoResponse(
        String id,
        String alertaId,
        String reportadoPor,
        UbicacionResponse ubicacion,
        String descripcion,
        String fotoUrl,
        EstadoVerificacion estado,
        String comentariosAdmin,
        Date creadoEn
) {
    public static AvistamientoResponse from(Avistamiento a) {
        UbicacionResponse ubic = a.getUbicacion() == null
                ? null
                : new UbicacionResponse("Point",
                        List.of(a.getUbicacion().getX(), a.getUbicacion().getY())); // [lng, lat]
        return new AvistamientoResponse(
                a.getId(), a.getAlertaId(), a.getReportadoPor(), ubic, a.getDescripcion(),
                a.getFotoUrl(), a.getEstado(), a.getComentariosAdmin(), a.getCreadoEn());
    }

    /** Ubicación en formato GeoJSON estándar para la salida. */
    public record UbicacionResponse(String type, List<Double> coordinates) {
    }
}
