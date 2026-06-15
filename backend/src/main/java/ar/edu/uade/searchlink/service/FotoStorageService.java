package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.exception.OperacionInvalidaException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda en disco las fotos subidas y resuelve el directorio único donde viven, para que la
 * escritura (este servicio) y la lectura (ResourceHandler en {@code WebConfig}) no se
 * desincronicen. NO asocia el archivo a ninguna identidad: sólo persiste bytes.
 */
@Service
public class FotoStorageService {

    /** Content-types aceptados → extensión con la que se guarda cada uno. */
    private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path directorio;

    public FotoStorageService(@Value("${searchlink.uploads.dir:uploads}") String dir) {
        this.directorio = Paths.get(dir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void inicializar() {
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de uploads: " + directorio, e);
        }
    }

    /** Directorio absoluto donde se sirven las fotos; lo usa el ResourceHandler. */
    public Path getDirectorio() {
        return directorio;
    }

    /**
     * Valida tipo y contenido, guarda la foto con un nombre único e impredecible (UUID) y
     * devuelve SÓLO el nombre del archivo generado. La extensión sale del content-type validado,
     * NUNCA del nombre original del cliente: evita path traversal y extensiones arbitrarias.
     */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new OperacionInvalidaException("La foto es obligatoria y no puede estar vacía");
        }
        String extension = TIPOS_PERMITIDOS.get(archivo.getContentType());
        if (extension == null) {
            throw new OperacionInvalidaException(
                    "Tipo de archivo no permitido. Sólo se aceptan JPEG, PNG o WebP");
        }

        String nombre = UUID.randomUUID() + extension;
        Path destino = directorio.resolve(nombre).normalize();
        // Defensa en profundidad: el destino tiene que quedar DENTRO del directorio de uploads.
        if (!destino.startsWith(directorio)) {
            throw new OperacionInvalidaException("Ruta de archivo inválida");
        }
        try {
            archivo.transferTo(destino);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto", e);
        }
        return nombre;
    }
}
