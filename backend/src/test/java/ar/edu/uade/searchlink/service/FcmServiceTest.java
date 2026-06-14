package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.model.Alerta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de FcmService SIN contexto Spring y SIN credencial Firebase: como no se
 * inicializa ningún FirebaseApp en este JVM de test, el envío es no-op. Verifica la tolerancia:
 * devuelve lista vacía y NUNCA lanza. El camino "habilitado" (envío real) requiere credencial y
 * queda fuera del alcance de los tests.
 */
class FcmServiceTest {

    private final FcmService fcmService = new FcmService();

    @Test
    void sinCredencial_devuelveVaciaYNoLanza() {
        Alerta alerta = Alerta.builder().id("a1").nombreMenor("Juan").build();

        List<String> muertos = fcmService.enviarAlerta(List.of("tk-1", "tk-2"), alerta);

        assertThat(muertos).isEmpty();
    }

    @Test
    void tokensVaciosONull_devuelveVacia() {
        Alerta alerta = Alerta.builder().id("a1").build();

        assertThat(fcmService.enviarAlerta(List.of(), alerta)).isEmpty();
        assertThat(fcmService.enviarAlerta(null, alerta)).isEmpty();
    }
}
