package ar.edu.uade.searchlink.exception;

import ar.edu.uade.searchlink.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El mapeo DuplicateKeyException → 409 se prueba en aislamiento: forzar una colisión real del
 * índice único requiere una carrera concurrente no determinística. Acá se verifica que el
 * handler traduce la excepción a 409 (no 500) con el formato uniforme.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicateKeyExceptionSeTraduceA409() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/usuarios");

        ResponseEntity<ErrorResponse> resp =
                handler.handleDuplicateKey(new DuplicateKeyException("E11000 duplicate key"), req);

        assertThat(resp.getStatusCode().value()).isEqualTo(409);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status()).isEqualTo(409);
        assertThat(resp.getBody().error()).isEqualTo("Conflict");
        assertThat(resp.getBody().path()).isEqualTo("/api/usuarios");
    }
}
