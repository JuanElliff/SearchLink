package ar.edu.uade.searchlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Se excluye UserDetailsServiceAutoConfiguration: no usamos el UserDetailsService por
 * defecto (la autenticación es por JWT, validada en JwtAuthenticationFilter). Sin esta
 * exclusión Spring Boot crea un usuario in-memory y loguea en el arranque un
 * "Using generated security password: ...", que es ruido y queda mal en la demo.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SearchLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchLinkApplication.class, args);
    }
}
