package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.dto.RegistrarDispositivoRequest;
import ar.edu.uade.searchlink.exception.RecursoNoEncontradoException;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DispositivoService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Upsert del token FCM dentro de la lista embebida del PROPIO usuario autenticado.
     *
     * `userId` viene del JWT (Authentication.getName()), NO del cliente: el dueño es siempre quien
     * hace el request. Idempotente por `fcmToken`: re-registrar el mismo token no duplica, sólo
     * refresca `ultimoUso`/`activo`. Devuelve el dispositivo afectado (alta o el ya existente).
     */
    public Usuario.Dispositivo registrar(RegistrarDispositivoRequest req, String userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + userId));

        // La lista puede venir null al materializar desde Mongo (mismo guard que AlertaService).
        List<Usuario.Dispositivo> dispositivos = usuario.getDispositivos();
        if (dispositivos == null) {
            dispositivos = new ArrayList<>();
            usuario.setDispositivos(dispositivos);
        }

        Date ahora = new Date();
        Usuario.Dispositivo dispositivo = dispositivos.stream()
                .filter(d -> req.fcmToken().equals(d.getFcmToken()))
                .findFirst()
                .orElse(null);

        if (dispositivo != null) {
            // Ya registrado: refrescar, sin agregar duplicado.
            dispositivo.setUltimoUso(ahora);
            dispositivo.setActivo(true);
        } else {
            dispositivo = Usuario.Dispositivo.builder()
                    .fcmToken(req.fcmToken())
                    .plataforma(req.plataforma())
                    .activo(true)
                    .registradoEn(ahora)
                    .ultimoUso(ahora)
                    .build();
            dispositivos.add(dispositivo);
        }

        usuarioRepository.save(usuario);
        return dispositivo;
    }
}
