package ar.edu.uade.searchlink.exception;

/** El email ya está registrado. Se traduce a HTTP 409 Conflict. */
public class EmailDuplicadoException extends RuntimeException {
    public EmailDuplicadoException(String email) {
        super("Ya existe un usuario registrado con el email: " + email);
    }
}
