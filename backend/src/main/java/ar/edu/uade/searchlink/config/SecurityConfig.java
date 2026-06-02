package ar.edu.uade.searchlink.config;

import ar.edu.uade.searchlink.dto.ErrorResponse;
import ar.edu.uade.searchlink.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Configuración de seguridad: cadena de filtros stateless con JWT, BCrypt y autorización
 * por roles. @EnableMethodSecurity habilita @PreAuthorize en los controllers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    /** BCrypt con cost factor 10 (default). Mismo encoder para hashear y verificar. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF deshabilitado a propósito: es una API REST stateless que autentica
                // con un JWT en el header Authorization, no con cookies de sesión. CSRF
                // explota cookies enviadas automáticamente por el browser; como acá el token
                // viaja en un header que el atacante no puede forzar desde otro origen, no
                // hay superficie CSRF. Habilitarlo sólo agregaría tokens CSRF sin beneficio.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Públicos ──────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()   // registro (fuerza ESTANDAR)
                        .requestMatchers(HttpMethod.POST, "/api/sesiones").permitAll()    // login

                        // ── Gestión de usuarios: sólo ADMIN ───────────────────────────
                        // El ADMIN es quien administra cuentas: da de alta operadores y otros admins.
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/operador").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/admin").hasRole("ADMIN")

                        // ── Mutaciones de alertas: sólo OPERADOR ──────────────────────
                        // SEPARACIÓN DE RESPONSABILIDADES (intencional, defensa ítem 16):
                        // emitir/gestionar alertas es un acto operativo de la autoridad, NO del
                        // administrador de la app. Por eso ADMIN NO puede crear/editar alertas:
                        // sólo OPERADOR. Quien gobierna las cuentas (ADMIN) no opera el sistema de
                        // alertas, y quien opera alertas (OPERADOR) no gobierna las cuentas.
                        .requestMatchers(HttpMethod.POST, "/api/alertas").hasRole("OPERADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/alertas/**").hasRole("OPERADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/alertas/**").hasRole("OPERADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/alertas/**").hasRole("OPERADOR")

                        // ── Avistamientos ─────────────────────────────────────────────
                        // Reportar un avistamiento: cualquier usuario autenticado (la ciudadanía
                        // ESTANDAR es la que reporta). El endpoint todavía no existe (otro bloque),
                        // la regla queda lista para cuando se implemente POST /api/avistamientos.
                        .requestMatchers(HttpMethod.POST, "/api/avistamientos").authenticated()
                        // Verificar/descartar un avistamiento: OPERADOR y ADMIN (moderación). El
                        // verbo/path definitivo se decide al implementar el endpoint; regla pendiente:
                        //   .requestMatchers(HttpMethod.PATCH, "/api/avistamientos/*/estado").hasAnyRole("OPERADOR", "ADMIN")

                        // ── Lecturas: cualquier autenticado ───────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/alertas/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/avistamientos/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").authenticated()

                        // ── Default ───────────────────────────────────────────────────
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // 401 cuando falta auth o el token es inválido/expirado.
                        .authenticationEntryPoint((req, res, e) ->
                                escribirError(req, res, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Unauthorized", "Autenticación requerida o token inválido"))
                        // 403 cuando el usuario está autenticado pero no tiene el rol.
                        .accessDeniedHandler((req, res, e) ->
                                escribirError(req, res, HttpServletResponse.SC_FORBIDDEN,
                                        "Forbidden", "No tenés permisos para acceder a este recurso"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** CORS para el frontend de desarrollo (Vite en :5173). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Escribe un ErrorResponse JSON con el mismo formato que el @RestControllerAdvice. */
    private void escribirError(HttpServletRequest req, HttpServletResponse res,
                               int status, String error, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        ErrorResponse body = ErrorResponse.of(status, error, message, req.getRequestURI());
        objectMapper.writeValue(res.getWriter(), body);
    }
}
