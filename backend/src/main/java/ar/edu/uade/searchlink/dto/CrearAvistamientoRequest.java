package ar.edu.uade.searchlink.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request de reporte de avistamiento: SÓLO los campos que el cliente puede aportar.
 *
 * Deliberadamente NO incluye `reportadoPor`, `estado`, `creadoEn` ni `id`. Esos los fija el
 * servidor; en particular `reportadoPor` sale del token del usuario autenticado (no del body),
 * para que el cliente no pueda falsear quién reportó. Cualquier campo extra en el JSON se ignora.
 */
public record CrearAvistamientoRequest(

        @NotBlank(message = "El id de la alerta es obligatorio")
        String alertaId,

        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,

        @NotNull(message = "La ubicación es obligatoria")
        @Valid
        UbicacionRequest ubicacion,

        String fotoUrl
) {
}
