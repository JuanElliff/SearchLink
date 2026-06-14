package ar.edu.uade.searchlink.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Inicialización (opcional) de FirebaseApp para el envío de push FCM.
 *
 * La credencial se provee por la propiedad `firebase.credentials.path` (env var
 * FIREBASE_CREDENTIALS_PATH). Comportamiento:
 *  - propiedad con un path a un archivo existente → inicializa FirebaseApp UNA sola vez → FCM habilitado.
 *  - propiedad vacía o archivo inexistente → NO inicializa, sólo log WARN → FCM deshabilitado.
 *
 * En ningún caso falla el arranque: sin credencial la app levanta igual y el push queda como no-op
 * (ver FcmService). Esto permite correr tests / `docker compose up` sin proyecto Firebase.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FCM deshabilitado: sin credencial (firebase.credentials.path vacío)");
            return;
        }

        File archivo = new File(credentialsPath);
        if (!archivo.exists()) {
            log.warn("FCM deshabilitado: sin credencial (archivo no encontrado: {})", credentialsPath);
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FCM habilitado: FirebaseApp ya estaba inicializado");
            return;
        }

        try (FileInputStream stream = new FileInputStream(archivo)) {
            FirebaseOptions opciones = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();
            FirebaseApp.initializeApp(opciones);
            log.info("FCM habilitado: FirebaseApp inicializado desde {}", credentialsPath);
        } catch (IOException e) {
            // No se propaga: la app debe arrancar igual aunque la credencial sea ilegible.
            log.error("FCM deshabilitado: error inicializando FirebaseApp desde {}", credentialsPath, e);
        }
    }
}
