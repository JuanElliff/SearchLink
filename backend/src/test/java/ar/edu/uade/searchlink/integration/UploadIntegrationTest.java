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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración del upload de fotos contra la cadena real (filtro JWT + SecurityFilterChain
 * + @PreAuthorize + controller + storage en disco). El foco es la MATRIZ DE AUTORIZACIÓN, que es
 * lo sensible de este bloque:
 *   - /api/uploads/alertas       → SÓLO OPERADOR (ESTANDAR y ADMIN reciben 403; anónimo, 401).
 *   - /api/uploads/avistamientos → cualquier AUTENTICADO (ESTANDAR/OPERADOR ok; anónimo, 401).
 * También cubre validación de tipo (415→400 de negocio) y que la foto servida en /uploads/** es
 * pública (GET sin token). El directorio de uploads se redirige a target/ para no ensuciar el repo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "searchlink.uploads.dir=target/test-uploads")
class UploadIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private UsuarioService usuarioService;

    private String tokenOperador;
    private String tokenEstandar;
    private String tokenAdmin;

    @BeforeEach
    void setUp() throws Exception {
        usuarioRepository.deleteAll();
        usuarioService.registrarOperador(
                new RegistroUsuarioRequest("Op", "op@x.com", "password123", -34.6037, -58.3816));
        usuarioService.registrar(
                new RegistroUsuarioRequest("Ciudadano", "ciu@x.com", "password123", -34.6037, -58.3816));
        usuarioService.registrarAdmin(
                new RegistroUsuarioRequest("Admin", "adm@x.com", "password123", -34.6037, -58.3816));
        tokenOperador = login("op@x.com", "password123");
        tokenEstandar = login("ciu@x.com", "password123");
        tokenAdmin = login("adm@x.com", "password123");
    }

    private String login(String email, String password) throws Exception {
        String resp = mvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("token").asText();
    }

    private MockMultipartFile fotoJpeg() {
        return new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
    }

    // ─────────────────────────── Upload de foto de ALERTA (sólo OPERADOR) ───────────────────────────

    @Test
    void operadorSubeFotoAlerta_da200ConUrlPublica() throws Exception {
        mvc.perform(multipart("/api/uploads/alertas").file(fotoJpeg())
                        .header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(containsString("/uploads/")))
                .andExpect(jsonPath("$.url").value(endsWith(".jpg")));
    }

    @Test
    void estandarNoPuedeSubirFotoAlerta_da403() throws Exception {
        mvc.perform(multipart("/api/uploads/alertas").file(fotoJpeg())
                        .header("Authorization", "Bearer " + tokenEstandar))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNoPuedeSubirFotoAlerta_da403() throws Exception {
        mvc.perform(multipart("/api/uploads/alertas").file(fotoJpeg())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonimoNoPuedeSubirFotoAlerta_da401() throws Exception {
        mvc.perform(multipart("/api/uploads/alertas").file(fotoJpeg()))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── Upload de foto de AVISTAMIENTO (cualquier autenticado) ───────────────────────

    @Test
    void estandarSubeFotoAvistamiento_da200() throws Exception {
        mvc.perform(multipart("/api/uploads/avistamientos").file(fotoJpeg())
                        .header("Authorization", "Bearer " + tokenEstandar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(containsString("/uploads/")));
    }

    @Test
    void operadorSubeFotoAvistamiento_da200() throws Exception {
        mvc.perform(multipart("/api/uploads/avistamientos").file(fotoJpeg())
                        .header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isOk());
    }

    @Test
    void anonimoNoPuedeSubirFotoAvistamiento_da401() throws Exception {
        mvc.perform(multipart("/api/uploads/avistamientos").file(fotoJpeg()))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────── Validación de tipo ───────────────────────────

    @Test
    void tipoNoPermitidoDa400() throws Exception {
        var txt = new MockMultipartFile("archivo", "nota.txt", "text/plain", "hola".getBytes());
        mvc.perform(multipart("/api/uploads/avistamientos").file(txt)
                        .header("Authorization", "Bearer " + tokenEstandar))
                .andExpect(status().isBadRequest());
    }

    @Test
    void archivoVacioDa400() throws Exception {
        var vacio = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[0]);
        mvc.perform(multipart("/api/uploads/avistamientos").file(vacio)
                        .header("Authorization", "Bearer " + tokenEstandar))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────── La foto servida es pública ───────────────────────────

    @Test
    void fotoSubidaSeSirvePublicamenteSinToken() throws Exception {
        String resp = mvc.perform(multipart("/api/uploads/alertas").file(fotoJpeg())
                        .header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String url = om.readTree(resp).get("url").asText();
        String path = URI.create(url).getPath(); // /uploads/<uuid>.jpg

        mvc.perform(get(path))   // sin Authorization
                .andExpect(status().isOk());
    }
}
