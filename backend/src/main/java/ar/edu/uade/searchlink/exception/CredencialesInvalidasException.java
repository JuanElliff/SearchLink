package ar.edu.uade.searchlink.exception;

/**
 * Credenciales de login inválidas. Se traduce a HTTP 401.
 *
 * SEGURIDAD: el mensaje es genérico y NO distingue entre "email inexistente" y
 * "password incorrecto". Revelar cuál de los dos falló le daría a un atacante un
 * oráculo para enumerar emails registrados. Mismo mensaje para ambos casos.
 */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
