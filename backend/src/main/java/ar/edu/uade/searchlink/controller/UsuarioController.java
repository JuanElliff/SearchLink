package ar.edu.uade.searchlink.controller;

import ar.edu.uade.searchlink.dto.RegistroUsuarioRequest;
import ar.edu.uade.searchlink.dto.UsuarioResponse;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /** Registro público. El rol se fuerza a ESTANDAR en el service. */
    @PostMapping
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroUsuarioRequest req) {
        Usuario creado = usuarioService.registrar(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(creado));
    }

    /** Alta de admin: sólo un ADMIN autenticado puede crear otro ADMIN. */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> registrarAdmin(@Valid @RequestBody RegistroUsuarioRequest req) {
        Usuario creado = usuarioService.registrarAdmin(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(creado));
    }

    /** Alta de operador: sólo un ADMIN autenticado puede crear un OPERADOR. */
    @PostMapping("/operador")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> registrarOperador(@Valid @RequestBody RegistroUsuarioRequest req) {
        Usuario creado = usuarioService.registrarOperador(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(creado));
    }

    /**
     * Detalle de un usuario. Nunca devuelve el passwordHash.
     *
     * PRIVACIDAD: cada usuario puede ver SU propio perfil; ADMIN y OPERADOR pueden ver el de
     * cualquiera (necesitan los datos —incluida la ubicación— para operar/moderar). Un
     * ESTANDAR pidiendo el perfil de otro recibe 403. Se resuelve con @PreAuthorize: el
     * principal del token es el userId, así que `#id == authentication.name` significa
     * "es mi propio perfil". Es la opción más limpia: declarativa y sin lógica en el service.
     */
    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.name or hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable String id) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.buscarPorId(id)));
    }
}
