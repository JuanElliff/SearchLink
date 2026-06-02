package ar.edu.uade.searchlink.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request de creación de alerta: SÓLO los campos que el cliente puede aportar.
 *
 * Deliberadamente NO incluye campos server-side: `id`, `estado`, `creada_en`, `expira_en`
 * ni `creado_por`. Esos los fija el servidor; en particular `creado_por` sale del token del
 * OPERADOR autenticado, no del body, para que el cliente no pueda falsear quién emitió la
 * alerta. Cualquier campo extra en el JSON se ignora (no se mapea a la entidad).
 */
public record CrearAlertaRequest(

        @NotBlank(message = "El nombre del menor es obligatorio")
        String nombreMenor,

        Integer edad,

        String descripcion,

        String fotoUrl,

        @NotNull(message = "La ubicación es obligatoria")
        @Valid
        UbicacionRequest ubicacion,

        @NotNull(message = "El radio es obligatorio")
        @Positive(message = "El radio debe ser mayor a 0")
        Double radioKm
) {
}
