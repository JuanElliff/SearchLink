package ar.edu.uade.searchlink.exception;

/** Operación de negocio inválida. Se traduce a HTTP 400 Bad Request. */
public class OperacionInvalidaException extends RuntimeException {
    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
