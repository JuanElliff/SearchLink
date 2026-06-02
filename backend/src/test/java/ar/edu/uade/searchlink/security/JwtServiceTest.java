package ar.edu.uade.searchlink.security;

import ar.edu.uade.searchlink.model.RolUsuario;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios de JwtService en aislamiento (sin contexto de Spring): se instancia
 * directamente con secret y expiración por constructor.
 */
class JwtServiceTest {

    private static final String SECRET = "secret-de-test-con-bastante-mas-de-32-bytes-1234567890";

    private final JwtService jwtService = new JwtService(SECRET, 86_400_000L); // 24h

    @Test
    void generaYParseaTokenValidoConTodosLosClaims() {
        String token = jwtService.generarToken("user-123", "ana@searchlink.dev", RolUsuario.ADMIN);

        assertThat(jwtService.esValido(token)).isTrue();
        assertThat(jwtService.extraerUserId(token)).isEqualTo("user-123");
        assertThat(jwtService.extraerEmail(token)).isEqualTo("ana@searchlink.dev");
        assertThat(jwtService.extraerRol(token)).isEqualTo(RolUsuario.ADMIN);
    }

    @Test
    void rechazaTokenExpirado() {
        // Servicio con expiración negativa: el token nace ya vencido.
        JwtService emisorExpirado = new JwtService(SECRET, -1_000L);
        String tokenExpirado = emisorExpirado.generarToken("u", "e@e.com", RolUsuario.ESTANDAR);

        assertThat(jwtService.esValido(tokenExpirado)).isFalse();
        assertThatThrownBy(() -> jwtService.parsear(tokenExpirado))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rechazaTokenConFirmaAlterada() {
        String token = jwtService.generarToken("u", "e@e.com", RolUsuario.ESTANDAR);
        // Alterar el último carácter (parte de la firma) invalida la verificación HMAC.
        char ultimo = token.charAt(token.length() - 1);
        char reemplazo = (ultimo == 'A') ? 'B' : 'A';
        String alterado = token.substring(0, token.length() - 1) + reemplazo;

        assertThat(jwtService.esValido(alterado)).isFalse();
    }

    @Test
    void rechazaTokenFirmadoConOtroSecret() {
        JwtService otroEmisor =
                new JwtService("otro-secret-totalmente-distinto-de-mas-de-32-bytes-xyz", 86_400_000L);
        String tokenAjeno = otroEmisor.generarToken("u", "e@e.com", RolUsuario.ADMIN);

        // Firma válida para otroEmisor, pero no para nuestro jwtService.
        assertThat(jwtService.esValido(tokenAjeno)).isFalse();
    }
}
