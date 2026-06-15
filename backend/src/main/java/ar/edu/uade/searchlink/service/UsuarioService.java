package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.dto.RegistroUsuarioRequest;
import ar.edu.uade.searchlink.exception.CredencialesInvalidasException;
import ar.edu.uade.searchlink.exception.EmailDuplicadoException;
import ar.edu.uade.searchlink.exception.OperacionInvalidaException;
import ar.edu.uade.searchlink.exception.RecursoNoEncontradoException;
import ar.edu.uade.searchlink.model.RolUsuario;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registro público de un usuario.
     *
     * SEGURIDAD — ROL FORZADO (defendible en ítem 16):
     * El rol se asigna SIEMPRE a {@link RolUsuario#ESTANDAR}, sin importar nada del
     * input. El DTO {@link RegistroUsuarioRequest} ni siquiera tiene campo `rol`, así
     * que un cliente no puede pedir ADMIN; y aunque el DTO cambiara, este método ignora
     * cualquier rol entrante y hardcodea ESTANDAR. Es la barrera contra escalación de
     * privilegios por auto-registro. La creación de ADMIN va por {@link #registrarAdmin}
     * (endpoint protegido) o por el seed de Mongo.
     */
    public Usuario registrar(RegistroUsuarioRequest req) {
        return crear(req, RolUsuario.ESTANDAR);
    }

    /**
     * Alta de un administrador. Sólo debe invocarse desde un endpoint protegido con
     * @PreAuthorize ADMIN — la autorización vive en la capa web, no acá.
     */
    public Usuario registrarAdmin(RegistroUsuarioRequest req) {
        return crear(req, RolUsuario.ADMIN);
    }

    /**
     * Alta de un operador (autoridad que emite alertas). Igual que registrarAdmin: la
     * autorización (sólo ADMIN) se aplica en la capa web vía @PreAuthorize.
     */
    public Usuario registrarOperador(RegistroUsuarioRequest req) {
        return crear(req, RolUsuario.OPERADOR);
    }

    private Usuario crear(RegistroUsuarioRequest req, RolUsuario rol) {
        usuarioRepository.findByEmail(req.email()).ifPresent(u -> {
            throw new EmailDuplicadoException(req.email());
        });

        Date ahora = new Date();
        Usuario.UsuarioBuilder builder = Usuario.builder()
                .nombre(req.nombre())
                .email(req.email())
                // El password se guarda SIEMPRE hasheado con BCrypt, nunca en claro.
                .passwordHash(passwordEncoder.encode(req.password()))
                .rol(rol)
                .activo(true)
                .creadoEn(ahora)
                .actualizadoEn(ahora);

        // GeoJSON usa orden [longitud, latitud] → GeoJsonPoint(x=lng, y=lat).
        if (req.latitud() != null && req.longitud() != null) {
            builder.ubicacionPrecargada(new GeoJsonPoint(req.longitud(), req.latitud()));
        }

        return usuarioRepository.save(builder.build());
    }

    public Usuario buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }

    /** Devuelve todos los usuarios del sistema (sin paginación, escala demo). Solo para ADMIN. */
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    /**
     * Activa o desactiva el flag `activo` de un usuario.
     *
     * @param id      id del usuario a modificar
     * @param activo  nuevo valor del flag
     * @param adminId id del ADMIN que realiza la operación (del JWT)
     * @throws RecursoNoEncontradoException si el usuario no existe (→ 404)
     * @throws OperacionInvalidaException   si un ADMIN intenta desactivarse a sí mismo (→ 400)
     */
    public Usuario cambiarActivo(String id, boolean activo, String adminId) {
        Usuario usuario = buscarPorId(id);
        if (!activo && id.equals(adminId)) {
            throw new OperacionInvalidaException("Un ADMIN no puede desactivarse a sí mismo");
        }
        usuario.setActivo(activo);
        return usuarioRepository.save(usuario);
    }

    /**
     * Valida credenciales para login. Lanza {@link CredencialesInvalidasException} —con
     * mensaje genérico— tanto si el email no existe como si el password no coincide como si
     * la cuenta está desactivada, para no filtrar qué usuarios están registrados ni cuáles
     * fueron dados de baja (no dar oráculo). Una cuenta con activo:false no puede loguear.
     */
    public Usuario autenticar(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(CredencialesInvalidasException::new);
        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        if (!usuario.isActivo()) {
            throw new CredencialesInvalidasException();
        }
        return usuario;
    }
}
