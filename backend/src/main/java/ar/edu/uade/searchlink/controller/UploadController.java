package ar.edu.uade.searchlink.controller;

import ar.edu.uade.searchlink.dto.UploadFotoResponse;
import ar.edu.uade.searchlink.service.FotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Upload de fotos. Endpoints SEPARADOS del create de alerta/avistamiento (decisión de diseño):
 * reciben el archivo, lo guardan y devuelven una URL absoluta; el create sigue mandando esa URL
 * como `fotoUrl` en su JSON habitual. Así no se transforma ningún endpoint existente a multipart.
 *
 * Autorización por ruta (también reforzada en SecurityConfig, defensa en profundidad):
 *   - /api/uploads/alertas        → sólo OPERADOR (igual que emitir una alerta).
 *   - /api/uploads/avistamientos  → cualquier autenticado (igual que reportar un avistamiento).
 * El upload NO asocia el archivo a ninguna identidad: sólo persiste bytes y devuelve su URL. La
 * autoría real (creadaPor/reportadoPor) la fija el create desde el token, no este endpoint.
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FotoStorageService fotoStorageService;

    /** Foto de una alerta: sólo OPERADOR. */
    @PostMapping("/alertas")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<UploadFotoResponse> subirFotoAlerta(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(guardarYResponder(archivo));
    }

    /** Foto de un avistamiento: cualquier usuario autenticado (la regla la fija SecurityConfig). */
    @PostMapping("/avistamientos")
    public ResponseEntity<UploadFotoResponse> subirFotoAvistamiento(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(guardarYResponder(archivo));
    }

    /** Guarda la foto y arma la URL absoluta pública (/uploads/&lt;archivo&gt;) a partir del request. */
    private UploadFotoResponse guardarYResponder(MultipartFile archivo) {
        String nombre = fotoStorageService.guardar(archivo);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(nombre)
                .toUriString();
        return new UploadFotoResponse(url);
    }
}
