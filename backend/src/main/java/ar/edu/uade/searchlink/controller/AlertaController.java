package ar.edu.uade.searchlink.controller;

import ar.edu.uade.searchlink.model.Alerta;
import ar.edu.uade.searchlink.model.EstadoAlerta;
import ar.edu.uade.searchlink.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    // Emitir/gestionar alertas es sólo del OPERADOR (ver separación de responsabilidades
    // en SecurityConfig). @PreAuthorize duplica la regla de URL como defensa en profundidad.
    @PostMapping
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<Alerta> crear(@RequestBody Alerta alerta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertaService.crear(alerta));
    }

    @GetMapping
    public ResponseEntity<List<Alerta>> listarActivas() {
        return ResponseEntity.ok(alertaService.listarActivas());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<Alerta> cambiarEstado(
            @PathVariable String id,
            @RequestParam EstadoAlerta estado) {
        return ResponseEntity.ok(alertaService.cambiarEstado(id, estado));
    }
}
