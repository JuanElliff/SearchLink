package ar.edu.uade.searchlink.exception;

import ar.edu.uade.searchlink.dto.ErrorResponse;
import ar.edu.uade.searchlink.dto.ErrorResponse.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Traduce excepciones a respuestas HTTP con el formato uniforme de {@link ErrorResponse}.
 *
 * Nota: los 401/403 que se originan dentro de la cadena de filtros de Spring Security
 * (antes del DispatcherServlet) los maneja SecurityConfig (authenticationEntryPoint /
 * accessDeniedHandler) con el MISMO formato. Acá se cubren los que surgen ya dentro del
 * controller — incluida la AccessDeniedException de @PreAuthorize a nivel de método.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Validación @Valid fallida → 400 con la lista de campos inválidos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        List<FieldErrorDetail> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Hay campos inválidos en la solicitud", req.getRequestURI(), campos);
        return ResponseEntity.badRequest().body(body);
    }

    /** Email ya registrado → 409 (chequeo de aplicación, antes de tocar la DB). */
    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleEmailDuplicado(EmailDuplicadoException ex,
                                                             HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), req);
    }

    /**
     * Violación de índice único en Mongo → 409. Cierra la ventana de carrera del registro:
     * si dos requests concurrentes pasan el chequeo de aplicación con el mismo email, el
     * índice único de la DB rechaza el segundo save y acá lo traducimos a 409 (no 500).
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateKeyException ex,
                                                            HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT, "Conflict",
                "Ya existe un registro con un valor único duplicado", req);
    }

    /** Credenciales inválidas → 401 (mensaje genérico). */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> handleCredenciales(CredencialesInvalidasException ex,
                                                            HttpServletRequest req) {
        return construir(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), req);
    }

    /** Acceso denegado (rol insuficiente vía @PreAuthorize) → 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccessDeniedException ex,
                                                             HttpServletRequest req) {
        return construir(HttpStatus.FORBIDDEN, "Forbidden",
                "No tenés permisos para acceder a este recurso", req);
    }

    /** Recurso inexistente → 404. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RecursoNoEncontradoException ex,
                                                            HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req);
    }

    /** Operación de negocio inválida (ej. ADMIN intentando desactivarse a sí mismo) → 400. */
    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleOperacionInvalida(OperacionInvalidaException ex,
                                                                 HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req);
    }

    /** Cualquier excepción no controlada → 500. NO se filtra el stacktrace al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerica(Exception ex, HttpServletRequest req) {
        // El detalle queda en el log del servidor; el cliente recibe sólo un mensaje genérico.
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Ocurrió un error inesperado", req);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String error,
                                                   String message, HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.of(status.value(), error, message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
