package ar.edu.uade.searchlink.controller;

import ar.edu.uade.searchlink.dto.DispositivoResponse;
import ar.edu.uade.searchlink.dto.RegistrarDispositivoRequest;
import ar.edu.uade.searchlink.service.DispositivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

    private final DispositivoService dispositivoService;

    /**
     * Registrar/actualizar el token FCM del usuario autenticado. Path PLANO: el dueño es SIEMPRE
     * el del JWT (Authentication.getName() == userId), NUNCA viene del body ni del path. Upsert
     * idempotente: re-registrar el mismo token no duplica. Devuelve 200 (no 201) porque puede ser
     * alta o actualización. Cae en anyRequest().authenticated() de SecurityConfig (cualquier
     * autenticado registra su propio token); sin regla extra.
     */
    @PostMapping
    public ResponseEntity<DispositivoResponse> registrar(@Valid @RequestBody RegistrarDispositivoRequest req,
                                                         Authentication authentication) {
        var dispositivo = dispositivoService.registrar(req, authentication.getName());
        return ResponseEntity.ok(DispositivoResponse.from(dispositivo));
    }
}
