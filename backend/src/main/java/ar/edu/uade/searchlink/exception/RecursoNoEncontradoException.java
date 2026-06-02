package ar.edu.uade.searchlink.exception;

/** Recurso inexistente. Se traduce a HTTP 404 Not Found. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
