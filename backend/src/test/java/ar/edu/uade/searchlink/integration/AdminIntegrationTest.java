package ar.edu.uade.searchlink.integration;

import ar.edu.uade.searchlink.dto.RegistroUsuarioRequest;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del panel ADMIN: listar usuarios y activar/desactivar.
 * Ejercita la cadena real: JWT + SecurityFilterChain + @PreAuthorize + controllers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioService usuarioService;

    private String tokenAdmin;
    private String idAdmin;
    private String tokenOperador;
    private String tokenEstandar;
    private String idOtroUsuario;

    @BeforeEach
    void setUp() throws Exception {
        usuarioRepository.deleteAll();

        usuarioService.registrarAdmin(
                new RegistroUsuarioRequest("Admin", "admin@x.com", "password123", -34.6, -58.3));
        usuarioService.registrarOperador(
                new RegistroUsuarioRequest("Op", "op@x.com", "password123", -34.6, -58.3));
        usuarioService.registrar(
                new RegistroUsuarioRequest("Ciu", "ciu@x.com", "password123", -34.6, -58.3));

        // Login del admin: extraemos token E id del usuario desde LoginResponse.
        String respAdmin = login("admin@x.com", "password123");
        tokenAdmin = om.readTree(respAdmin).get("token").asText();
        idAdmin    = om.readTree(respAdmin).get("usuario").get("id").asText();

        tokenOperador = om.readTree(login("op@x.com",  "password123")).get("token").asText();
        tokenEstandar = om.readTree(login("ciu@x.com", "password123")).get("token").asText();
        idOtroUsuario = usuarioRepository.findByEmail("op@x.com").orElseThrow().getId();
    }

    private String login(String email, String password) throws Exception {
        return mvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ── GET /api/usuarios ──────────────────────────────────────────────────────

    @Test
    void adminListaUsuarios_da200ConTodos() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].email",
                        org.hamcrest.Matchers.hasItems("admin@x.com", "op@x.com", "ciu@x.com")));
    }

    @Test
    void operadorListaUsuarios_da403() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isForbidden());
    }

    @Test
    void estandarListaUsuarios_da403() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenEstandar))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /api/usuarios/{id}/activo ───────────────────────────────────────

    @Test
    void adminDesactivaOtroUsuario_da200YActivoFalse() throws Exception {
        mvc.perform(patch("/api/usuarios/" + idOtroUsuario + "/activo")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("activo", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.id").value(idOtroUsuario));
    }

    @Test
    void noAdminCambiaActivo_da403() throws Exception {
        mvc.perform(patch("/api/usuarios/" + idOtroUsuario + "/activo")
                        .header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("activo", false))))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchActivoIdInexistente_da404() throws Exception {
        mvc.perform(patch("/api/usuarios/no-existe-9999/activo")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("activo", false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void adminSeDesactivaASiMismo_da400() throws Exception {
        mvc.perform(patch("/api/usuarios/" + idAdmin + "/activo")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("activo", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
