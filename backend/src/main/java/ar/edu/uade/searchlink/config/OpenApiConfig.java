package ar.edu.uade.searchlink.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI (Swagger UI). Declara un esquema de seguridad
 * HTTP Bearer ("bearerAuth") y lo aplica globalmente, de modo que el botón "Authorize" de
 * Swagger UI permita pegar el JWT una vez y que se mande como `Authorization: Bearer <token>`
 * en todas las requests de prueba.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI searchLinkOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SearchLink API")
                        .version("1.0")
                        .description("API REST de SearchLink: alertas geolocalizadas de menores desaparecidos."))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .name(ESQUEMA_BEARER)));
    }
}
