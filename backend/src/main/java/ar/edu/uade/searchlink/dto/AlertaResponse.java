package ar.edu.uade.searchlink.dto;

import ar.edu.uade.searchlink.model.Alerta;
import ar.edu.uade.searchlink.model.EstadoAlerta;

import java.util.Date;
import java.util.List;

/**
 * DTO de salida de alerta. No expone la entidad cruda. La ubicación se devuelve como GeoJSON
 * estándar {type, coordinates:[lng,lat]}. Se incluye `creadoPor` (id del operador emisor) por
 * transparencia: es un dato fijado por el servidor, no sensible.
 */
public record AlertaResponse(
        String id,
        String nombreMenor,
        Integer edad,
        String descripcion,
        String fotoUrl,
        UbicacionResponse ubicacion,
        Double radioKm,
        EstadoAlerta estado,
        String creadoPor,
        Date creadaEn,
        Date expiraEn,
        Date actualizadaEn
) {
    public static AlertaResponse from(Alerta a) {
        UbicacionResponse ubic = a.getUbicacion() == null
                ? null
                : new UbicacionResponse("Point",
                        List.of(a.getUbicacion().getX(), a.getUbicacion().getY())); // [lng, lat]
        return new AlertaResponse(
                a.getId(), a.getNombreMenor(), a.getEdad(), a.getDescripcion(), a.getFotoUrl(),
                ubic, a.getRadioKm(), a.getEstado(), a.getCreadaPor(),
                a.getCreadaEn(), a.getExpiraEn(), a.getActualizadaEn());
    }

    /** Ubicación en formato GeoJSON estándar para la salida. */
    public record UbicacionResponse(String type, List<Double> coordinates) {
    }
}
