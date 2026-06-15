package ar.edu.uade.searchlink.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request de activación/desactivación de un usuario (PATCH /api/usuarios/{id}/activo).
 *
 * `activo` es Boolean (boxed, no primitivo) para que @NotNull pueda detectar la ausencia del
 * campo en el body y devolver 400 en lugar de tratar el null como false silenciosamente.
 */
public record CambiarActivoRequest(

        @NotNull(message = "El campo activo es obligatorio")
        Boolean activo
) {
}
