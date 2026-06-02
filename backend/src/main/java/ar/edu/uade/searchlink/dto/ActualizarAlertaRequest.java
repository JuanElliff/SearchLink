package ar.edu.uade.searchlink.dto;

import ar.edu.uade.searchlink.model.EstadoAlerta;
import jakarta.validation.constraints.Positive;

/**
 * Request de actualización parcial de alerta (PATCH).
 *
 * Campos editables: `estado` (activar/resolver/cancelar) y `radioKm`. Ambos opcionales:
 * sólo se aplican los que vengan no nulos. NO se puede tocar `creado_por`, `creada_en`,
 * `id` ni los demás campos server-side: no existen en este DTO.
 */
public record ActualizarAlertaRequest(

        EstadoAlerta estado,

        @Positive(message = "El radio debe ser mayor a 0")
        Double radioKm
) {
}
