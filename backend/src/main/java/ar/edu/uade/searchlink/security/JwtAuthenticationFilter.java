package ar.edu.uade.searchlink.security;

import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro que extrae el JWT del header Authorization: Bearer <token>, valida firma y
 * expiración, verifica que el usuario siga existiendo y ACTIVO, y popula el SecurityContext
 * con el userId (principal) y el rol (authority).
 *
 * Si no hay header o el token es inválido/expirado/malformado, o el usuario fue desactivado
 * o borrado después de emitir el token, NO autentica y deja seguir la cadena: la decisión de
 * devolver 401 la toma el AuthenticationEntryPoint cuando el endpoint requiere autenticación.
 * Un token roto NUNCA produce un 500.
 *
 * NOTA DE DISEÑO: se carga el usuario de la base en cada request autenticada (un lookup por
 * _id, indexado). Es un costo consciente: permite que desactivar una cuenta surta efecto
 * inmediato aunque el token siga vigente (no hay refresh ni blacklist en este alcance). El
 * rol que se aplica es el ACTUAL del usuario, no el "congelado" en el token.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIJO)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIJO.length());
        try {
            Claims claims = jwtService.parsear(token);
            String userId = claims.getSubject();

            Optional<Usuario> usuario = usuarioRepository.findById(userId);
            // El usuario debe existir y estar activo; si fue desactivado/borrado, el token
            // deja de valer aunque la firma sea válida.
            if (usuario.isPresent() && usuario.get().isActivo()) {
                var authority = new SimpleGrantedAuthority("ROLE_" + usuario.get().getRol().name());
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(authority));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                SecurityContextHolder.clearContext();
            }
        } catch (JwtException | IllegalArgumentException e) {
            // Token inválido/expirado/malformado: no autenticamos. La request sigue como
            // anónima; el entry point devolverá 401 si el recurso lo exige.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
