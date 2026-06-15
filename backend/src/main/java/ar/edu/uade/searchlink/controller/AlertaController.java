package ar.edu.uade.searchlink.controller;

import ar.edu.uade.searchlink.dto.ActualizarAlertaRequest;
import ar.edu.uade.searchlink.dto.AlertaResponse;
import ar.edu.uade.searchlink.dto.CrearAlertaRequest;
import ar.edu.uade.searchlink.service.AlertaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    /**
     * Emitir una alerta: sólo OPERADOR (ver separación de responsabilidades en SecurityConfig).
     * `creado_por` se toma del token (Authentication.getName() == userId), NUNCA del body: el
     * cliente no puede falsear quién emitió la alerta.
     */
    @PostMapping
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<AlertaResponse> crear(@Valid @RequestBody CrearAlertaRequest req,
                                                Authentication authentication) {
        var creada = alertaService.crear(req, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(AlertaResponse.from(creada));
    }

    @GetMapping
    public ResponseEntity<List<AlertaResponse>> listarActivas() {
        List<AlertaResponse> alertas = alertaService.listarActivas().stream()
                .map(AlertaResponse::from)
                .toList();
        return ResponseEntity.ok(alertas);
    }

    /** Detalle de una alerta por id (cualquier estado). Cualquier autenticado. 404 si no existe. */
    @GetMapping("/{id}")
    public ResponseEntity<AlertaResponse> obtener(@PathVariable String id) {
        return ResponseEntity.ok(AlertaResponse.from(alertaService.obtenerPorId(id)));
    }

    /** Actualización parcial (estado/radio): sólo OPERADOR. */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<AlertaResponse> actualizar(@PathVariable String id,
                                                     @Valid @RequestBody ActualizarAlertaRequest req) {
        return ResponseEntity.ok(AlertaResponse.from(alertaService.actualizar(id, req)));
    }
}
