package ar.edu.uade.searchlink.controller;

import ar.edu.uade.searchlink.dto.AvistamientoResponse;
import ar.edu.uade.searchlink.dto.CambiarEstadoAvistamientoRequest;
import ar.edu.uade.searchlink.dto.CrearAvistamientoRequest;
import ar.edu.uade.searchlink.service.AvistamientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avistamientos")
@RequiredArgsConstructor
public class AvistamientoController {

    private final AvistamientoService avistamientoService;

    /**
     * Reportar un avistamiento: cualquier usuario autenticado (la ciudadanía ESTANDAR es la que
     * reporta; ver SecurityConfig). `reportadoPor` se toma del token (Authentication.getName() ==
     * userId), NUNCA del body: el cliente no puede falsear quién reportó.
     */
    @PostMapping
    public ResponseEntity<AvistamientoResponse> crear(@Valid @RequestBody CrearAvistamientoRequest req,
                                                      Authentication authentication) {
        var creado = avistamientoService.crear(req, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(AvistamientoResponse.from(creado));
    }

    /** Listar los avistamientos de una alerta. Cualquier autenticado. */
    @GetMapping
    public ResponseEntity<List<AvistamientoResponse>> listarPorAlerta(@RequestParam String alertaId) {
        List<AvistamientoResponse> avistamientos = avistamientoService.listarPorAlerta(alertaId).stream()
                .map(AvistamientoResponse::from)
                .toList();
        return ResponseEntity.ok(avistamientos);
    }

    /** Obtener un avistamiento por id. Cualquier autenticado. 404 si no existe. */
    @GetMapping("/{id}")
    public ResponseEntity<AvistamientoResponse> obtener(@PathVariable String id) {
        return ResponseEntity.ok(AvistamientoResponse.from(avistamientoService.obtener(id)));
    }

    /** Moderación (verificar/descartar): sólo OPERADOR. 404 si el avistamiento no existe. */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<AvistamientoResponse> cambiarEstado(@PathVariable String id,
                                                              @Valid @RequestBody CambiarEstadoAvistamientoRequest req) {
        return ResponseEntity.ok(AvistamientoResponse.from(avistamientoService.cambiarEstado(id, req)));
    }
}
