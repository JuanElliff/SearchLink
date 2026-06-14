package ar.edu.uade.searchlink.dto;

import ar.edu.uade.searchlink.model.EstadoVerificacion;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Request de moderación de un avistamiento (PATCH /api/avistamientos/{id}/estado).
 *
 * Sólo el OPERADOR transiciona el estado. El destino válido es VERIFICADO o DESCARTADO: NO se
 * permite volver a PENDIENTE por este endpoint (PENDIENTE es el estado inicial server-side). El
 * comentario del operador es opcional.
 */
public record CambiarEstadoAvistamientoRequest(

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoVerificacion nuevoEstado,

        String comentariosAdmin
) {
    /**
     * Valida que el destino sea una decisión de moderación (VERIFICADO/DESCARTADO) y no un
     * regreso a PENDIENTE. Devuelve true cuando es null para no pisar el mensaje de @NotNull.
     */
    @AssertTrue(message = "El nuevo estado debe ser VERIFICADO o DESCARTADO")
    public boolean isNuevoEstadoModerable() {
        return nuevoEstado == null
                || nuevoEstado == EstadoVerificacion.VERIFICADO
                || nuevoEstado == EstadoVerificacion.DESCARTADO;
    }
}
