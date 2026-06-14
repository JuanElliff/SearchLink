package ar.edu.uade.searchlink.integration;

import ar.edu.uade.searchlink.dto.RegistroUsuarioRequest;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.repository.UsuarioRepository;
import ar.edu.uade.searchlink.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del alta de token FCM (POST /api/dispositivos) contra MongoDB embebido.
 * Ejercita la cadena real: filtro JWT + SecurityFilterChain + controller + upsert embebido en el
 * propio usuario del token. NO toca dispatch FCM.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DispositivoIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private UsuarioService usuarioService;

    private String token;
    private String email;

    @BeforeEach
    void setUp() throws Exception {
        usuarioRepository.deleteAll();
        email = "ciu@x.com";
        usuarioService.registrar(
                new RegistroUsuarioRequest("Ciudadano", email, "password123", -34.6037, -58.3816));
        token = login(email, "password123");
    }

    // ─────────────────────────── Helpers ───────────────────────────

    private String json(Object o) throws Exception {
        return om.writeValueAsString(o);
    }

    private String login(String email, String password) throws Exception {
        String resp = mvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("token").asText();
    }

    /** POST /api/dispositivos con el token del usuario. */
    private org.springframework.test.web.servlet.ResultActions registrar(String fcmToken, String plataforma)
            throws Exception {
        return mvc.perform(post("/api/dispositivos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("fcmToken", fcmToken, "plataforma", plataforma))));
    }

    private Usuario recargar() {
        return usuarioRepository.findByEmail(email).orElseThrow();
    }

    // ─────────────────────────── Tests ───────────────────────────

    @Test
    void registrarToken_da200YQuedaActivoEnLaLista() throws Exception {
        registrar("token-abc", "ANDROID")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plataforma").value("ANDROID"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.registradoEn").exists())
                .andExpect(jsonPath("$.ultimoUso").exists());

        Usuario u = recargar();
        assertThat(u.getDispositivos()).hasSize(1);
        Usuario.Dispositivo d = u.getDispositivos().get(0);
        assertThat(d.getFcmToken()).isEqualTo("token-abc");
        assertThat(d.isActivo()).isTrue();
    }

    @Test
    void registrarMismoTokenDosVeces_noCreceLista_yActualizaUltimoUso() throws Exception {
        registrar("token-abc", "ANDROID").andExpect(status().isOk());

        // Forzar un ultimoUso viejo para verificar de forma robusta que el segundo alta lo refresca.
        Usuario u1 = recargar();
        Date viejo = new Date(0);
        u1.getDispositivos().get(0).setUltimoUso(viejo);
        usuarioRepository.save(u1);

        registrar("token-abc", "ANDROID").andExpect(status().isOk());

        Usuario u2 = recargar();
        assertThat(u2.getDispositivos()).hasSize(1);                    // no duplica
        assertThat(u2.getDispositivos().get(0).getUltimoUso()).isAfter(viejo); // se actualizó
    }

    @Test
    void registrarTokenDistinto_laListaCreceADos() throws Exception {
        registrar("token-abc", "ANDROID").andExpect(status().isOk());
        registrar("token-xyz", "IOS").andExpect(status().isOk());

        assertThat(recargar().getDispositivos()).hasSize(2);
    }

    @Test
    void fcmTokenEnBlancoDa400() throws Exception {
        mvc.perform(post("/api/dispositivos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fcmToken", "", "plataforma", "ANDROID"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[*].campo", org.hamcrest.Matchers.hasItem("fcmToken")));
    }

    @Test
    void postSinTokenJwtDa401() throws Exception {
        mvc.perform(post("/api/dispositivos").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fcmToken", "token-abc", "plataforma", "ANDROID"))))
                .andExpect(status().isUnauthorized());
    }
}
