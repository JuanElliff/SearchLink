package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.dto.RegistroUsuarioRequest;
import ar.edu.uade.searchlink.exception.EmailDuplicadoException;
import ar.edu.uade.searchlink.model.RolUsuario;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;

    // PasswordEncoder real (no mock): queremos verificar que el hash es BCrypt válido.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder);
    }

    /**
     * EL HUECO DEL ROL ESTÁ TAPADO.
     *
     * El registro público siempre produce rol ESTANDAR. Nótese que el DTO
     * {@link RegistroUsuarioRequest} ni siquiera tiene un campo `rol`: un cliente no
     * tiene forma de pedir ADMIN en el body. Aun así, verificamos que el documento que
     * se persiste lleva ESTANDAR, que es la garantía de no-escalación de privilegios.
     */
    @Test
    void registroPublicoSiempreFuerzaRolEstandar() {
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroUsuarioRequest req =
                new RegistroUsuarioRequest("Juan Pérez", "juan@x.com", "password123", -34.60, -58.38);

        Usuario resultado = usuarioService.registrar(req);

        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().getRol())
                .as("el documento persistido debe quedar como ESTANDAR")
                .isEqualTo(RolUsuario.ESTANDAR);
        assertThat(resultado.getRol()).isEqualTo(RolUsuario.ESTANDAR);
    }

    @Test
    void passwordSeGuardaHasheadoNuncaEnClaro() {
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroUsuarioRequest req =
                new RegistroUsuarioRequest("Juan", "juan@x.com", "password123", -34.60, -58.38);

        usuarioService.registrar(req);

        verify(usuarioRepository).save(usuarioCaptor.capture());
        String hash = usuarioCaptor.getValue().getPasswordHash();
        assertThat(hash).isNotEqualTo("password123");
        assertThat(hash).startsWith("$2"); // prefijo BCrypt
        assertThat(passwordEncoder.matches("password123", hash)).isTrue();
    }

    @Test
    void registroConEmailDuplicadoLanzaExcepcionYNoPersiste() {
        when(usuarioRepository.findByEmail("juan@x.com"))
                .thenReturn(Optional.of(new Usuario()));

        RegistroUsuarioRequest req =
                new RegistroUsuarioRequest("Juan", "juan@x.com", "password123", -34.60, -58.38);

        assertThatThrownBy(() -> usuarioService.registrar(req))
                .isInstanceOf(EmailDuplicadoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarAdminProduceRolAdmin() {
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroUsuarioRequest req =
                new RegistroUsuarioRequest("Admin", "admin@x.com", "password123", -34.60, -58.38);

        Usuario resultado = usuarioService.registrarAdmin(req);

        assertThat(resultado.getRol()).isEqualTo(RolUsuario.ADMIN);
    }
}
