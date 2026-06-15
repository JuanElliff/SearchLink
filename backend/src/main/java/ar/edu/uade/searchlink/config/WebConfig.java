package ar.edu.uade.searchlink.config;

import ar.edu.uade.searchlink.service.FotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve las fotos subidas como recursos estáticos en /uploads/**. La ubicación en disco la
 * define {@link FotoStorageService} (la misma carpeta donde escribe el upload), para que
 * escritura y lectura no se desincronicen. El acceso es PÚBLICO a propósito (ver SecurityConfig):
 * las fotos de alertas son contenido de difusión.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FotoStorageService fotoStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = fotoStorageService.getDirectorio().toUri().toString(); // file:///.../uploads/
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
