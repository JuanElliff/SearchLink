package ar.edu.uade.searchlink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo de error uniforme para toda la API:
 * { timestamp, status, error, message, path, errors? }.
 * `errors` sólo aparece en respuestas de validación (lista de campos inválidos);
 * se omite del JSON en el resto de los casos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> errors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now().toString(), status, error, message, path, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path,
                                   List<FieldErrorDetail> errors) {
        return new ErrorResponse(Instant.now().toString(), status, error, message, path, errors);
    }

    /** Detalle de un campo que falló validación. */
    public record FieldErrorDetail(String campo, String mensaje) {
    }
}
