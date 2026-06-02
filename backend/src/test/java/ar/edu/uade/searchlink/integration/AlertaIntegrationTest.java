package ar.edu.uade.searchlink.integration;

import ar.edu.uade.searchlink.dto.RegistroUsuarioRequest;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.repository.AlertaRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del endpoint de alertas refactorizado a DTOs: creación con DTO válido,
 * `creado_por` server-side (no falsificable desde el body) y validación de entrada.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AlertaIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AlertaRepository alertaRepository;
    @Autowired
    private UsuarioService usuarioService;

    private String tokenOperador;
    private String idOperador;

    @BeforeEach
    void setUp() throws Exception {
        alertaRepository.deleteAll();
        usuarioRepository.deleteAll();
        Usuario operador = usuarioService.registrarOperador(
                new RegistroUsuarioRequest("Op", "op@x.com", "password123", -34.6037, -58.3816));
        idOperador = operador.getId();
        tokenOperador = login("op@x.com", "password123");
    }

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

    private Map<String, Object> ubicacion(double lat, double lng) {
        return Map.of("latitud", lat, "longitud", lng);
    }

    @Test
    void operadorCreaAlertaConDtoValido() throws Exception {
        var body = Map.of(
                "nombreMenor", "Juan Niño",
                "edad", 8,
                "descripcion", "visto por última vez en la plaza",
                "ubicacion", ubicacion(-34.6037, -58.3816),
                "radioKm", 10.0);

        mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))     // server-side
                .andExpect(jsonPath("$.creadoPor").value(idOperador)) // server-side, del token
                .andExpect(jsonPath("$.expiraEn").exists())           // server-side (TTL)
                .andExpect(jsonPath("$.ubicacion.type").value("Point"))
                .andExpect(jsonPath("$.ubicacion.coordinates[0]").value(-58.3816)) // [lng, lat]
                .andExpect(jsonPath("$.ubicacion.coordinates[1]").value(-34.6037));
    }

    @Test
    void creadoPorSeSeteaDelTokenNoDelBody() throws Exception {
        // El body intenta falsear creadoPor, id y estado. Todos deben ignorarse.
        var body = Map.of(
                "nombreMenor", "Juan Niño",
                "ubicacion", ubicacion(-34.6037, -58.3816),
                "radioKm", 5.0,
                "creadoPor", "id-falso-9999",
                "id", "id-hackeado",
                "estado", "RESUELTA");

        mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creadoPor").value(idOperador))      // del token
                .andExpect(jsonPath("$.creadoPor").value(org.hamcrest.Matchers.not("id-falso-9999")))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))           // server-side, no "RESUELTA"
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not("id-hackeado")));
    }

    @Test
    void alertaSinUbicacionDa400() throws Exception {
        var body = Map.of("nombreMenor", "Juan", "radioKm", 5.0); // sin ubicacion
        mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[*].campo", org.hamcrest.Matchers.hasItem("ubicacion")));
    }

    @Test
    void radioNegativoDa400() throws Exception {
        var body = Map.of("nombreMenor", "Juan",
                "ubicacion", ubicacion(-34.6037, -58.3816), "radioKm", -5.0);
        mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].campo", org.hamcrest.Matchers.hasItem("radioKm")));
    }

    @Test
    void latitudFueraDeRangoDa400() throws Exception {
        var body = Map.of("nombreMenor", "Juan",
                "ubicacion", ubicacion(200.0, -58.3816), "radioKm", 5.0);
        mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].campo",
                        org.hamcrest.Matchers.hasItem("ubicacion.latitud")));
    }
}
