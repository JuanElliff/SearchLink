package ar.edu.uade.searchlink.security;

import ar.edu.uade.searchlink.model.RolUsuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generación y validación de JWT firmados con HS256.
 *
 * Diseñado para ser testeable en aislamiento: el constructor recibe el secret y la
 * expiración por parámetro (vía @Value en runtime, valores directos en los unit tests),
 * sin depender de ningún contexto de Spring.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(
            @Value("${searchlink.jwt.secret}") String secret,
            @Value("${searchlink.jwt.expiration-ms}") long expirationMillis) {
        // HS256 exige una clave de al menos 256 bits (32 bytes). Si el secret es más
        // corto, Keys.hmacShaKeyFor lanza WeakKeyException explícita en el arranque
        // en vez de fallar silenciosamente.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /** Genera un token firmado con subject=userId y claims email+rol. */
    public String generarToken(String userId, String email, RolUsuario rol) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("rol", rol.name())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(key)
                .compact();
    }

    /**
     * Parsea y valida firma + expiración. Lanza JwtException si el token es inválido,
     * expirado o tiene la firma alterada. Los callers que no quieran manejar la
     * excepción pueden usar {@link #esValido(String)}.
     */
    public Claims parsear(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esValido(String token) {
        try {
            parsear(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extraerUserId(String token) {
        return parsear(token).getSubject();
    }

    public String extraerEmail(String token) {
        return parsear(token).get("email", String.class);
    }

    public RolUsuario extraerRol(String token) {
        return RolUsuario.valueOf(parsear(token).get("rol", String.class));
    }
}
