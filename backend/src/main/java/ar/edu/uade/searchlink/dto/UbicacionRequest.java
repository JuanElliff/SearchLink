package ar.edu.uade.searchlink.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Ubicación en el cuerpo de los requests, como par latitud/longitud explícito.
 *
 * Se usa {latitud, longitud} en vez del array GeoJSON crudo {type, coordinates:[lng,lat]}
 * por dos motivos: (1) permite validar el rango de cada coordenada de forma declarativa con
 * @DecimalMin/@DecimalMax (mismo criterio que el registro de usuarios), cosa que el array
 * posicional de GeoJSON no permite; (2) evita el footgun del orden [lng,lat] de GeoJSON.
 * El servidor construye el GeoJsonPoint con el orden correcto. La RESPUESTA sí expone la
 * ubicación como GeoJSON estándar {type, coordinates:[lng,lat]}.
 */
public record UbicacionRequest(

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
        Double longitud
) {
}
