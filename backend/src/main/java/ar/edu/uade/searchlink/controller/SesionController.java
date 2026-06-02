package ar.edu.uade.searchlink.controller;

import ar.edu.uade.searchlink.dto.LoginRequest;
import ar.edu.uade.searchlink.dto.LoginResponse;
import ar.edu.uade.searchlink.dto.UsuarioResponse;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.security.JwtService;
import ar.edu.uade.searchlink.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
public class SesionController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    /**
     * Login. Valida credenciales contra el hash BCrypt; si son correctas devuelve el JWT
     * y los datos básicos del usuario. Credenciales inválidas → 401 con mensaje genérico
     * (el service no distingue email inexistente de password incorrecto).
     */
    @PostMapping
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        Usuario usuario = usuarioService.autenticar(req.email(), req.password());
        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol());
        return ResponseEntity.ok(LoginResponse.de(token, UsuarioResponse.from(usuario)));
    }
}
