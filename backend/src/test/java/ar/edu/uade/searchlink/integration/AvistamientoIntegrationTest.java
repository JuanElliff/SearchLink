package ar.edu.uade.searchlink.integration;

import ar.edu.uade.searchlink.dto.RegistroUsuarioRequest;
import ar.edu.uade.searchlink.repository.AlertaRepository;
import ar.edu.uade.searchlink.repository.AvistamientoRepository;
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

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del bloque de avistamientos contra MongoDB embebido. Ejercita la cadena
 * real: filtro JWT + SecurityFilterChain + @PreAuthorize + controllers. Cubre: alta server-side
 * (estado PENDIENTE, reportadoPor del token), validación de alerta inexistente, listado por
 * alerta, y moderación (PATCH /estado) con su matriz de roles.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AvistamientoIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AlertaRepository alertaRepository;
    @Autowired
    private AvistamientoRepository avistamientoRepository;
    @Autowired
    private UsuarioService usuarioService;

    private String tokenOperador;
    private String tokenEstandar;
    private String alertaId;

    @BeforeEach
    void setUp() throws Exception {
        avistamientoRepository.deleteAll();
        alertaRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioService.registrarOperador(
                new RegistroUsuarioRequest("Op", "op@x.com", "password123", -34.6037, -58.3816));
        usuarioService.registrar(
                new RegistroUsuarioRequest("Ciudadano", "ciu@x.com", "password123", -34.6037, -58.3816));
        tokenOperador = login("op@x.com", "password123");
        tokenEstandar = login("ciu@x.com", "password123");

        // El operador publica una alerta a la que se atan los avistamientos.
        alertaId = crearAlerta();
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

    private Map<String, Object> ubicacion(double lat, double lng) {
        return Map.of("latitud", lat, "longitud", lng);
    }

    private String crearAlerta() throws Exception {
        var body = Map.of(
                "nombreMenor", "Juan Niño",
                "edad", 8,
                "descripcion", "visto por última vez en la plaza",
                "ubicacion", ubicacion(-34.6037, -58.3816),
                "radioKm", 10.0);
        String resp = mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("id").asText();
    }

    /** Crea un avistamiento como ESTANDAR y devuelve su id. */
    private String crearAvistamiento() throws Exception {
        var body = new HashMap<String, Object>();
        body.put("alertaId", alertaId);
        body.put("descripcion", "lo vi cerca de la estación");
        body.put("ubicacion", ubicacion(-34.6040, -58.3820));
        String resp = mvc.perform(post("/api/avistamientos").header("Authorization", "Bearer " + tokenEstandar)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("id").asText();
    }

    // ─────────────────────────── Alta ───────────────────────────

    @Test
    void estandarReportaAvistamiento_da201YEstadoPendiente() throws Exception {
        var body = new HashMap<String, Object>();
        body.put("alertaId", alertaId);
        body.put("descripcion", "lo vi cerca de la estación");
        body.put("ubicacion", ubicacion(-34.6040, -58.3820));

        mvc.perform(post("/api/avistamientos").header("Authorization", "Bearer " + tokenEstandar)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.alertaId").value(alertaId))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))     // server-side
                .andExpect(jsonPath("$.reportadoPor").exists())          // server-side, del token
                .andExpect(jsonPath("$.creadoEn").exists())              // server-side
                .andExpect(jsonPath("$.ubicacion.type").value("Point"))
                .andExpect(jsonPath("$.ubicacion.coordinates[0]").value(-58.3820)) // [lng, lat]
                .andExpect(jsonPath("$.ubicacion.coordinates[1]").value(-34.6040));
    }

    @Test
    void avistamientoConAlertaInexistenteDa404() throws Exception {
        var body = new HashMap<String, Object>();
        body.put("alertaId", "no-existe-9999");
        body.put("descripcion", "descripción válida");
        body.put("ubicacion", ubicacion(-34.6040, -58.3820));

        mvc.perform(post("/api/avistamientos").header("Authorization", "Bearer " + tokenEstandar)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─────────────────────────── Listado ───────────────────────────

    @Test
    void listarPorAlertaContieneElCreado() throws Exception {
        String id = crearAvistamiento();

        mvc.perform(get("/api/avistamientos").param("alertaId", alertaId)
                        .header("Authorization", "Bearer " + tokenEstandar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(id)))
                .andExpect(jsonPath("$[*].alertaId",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(alertaId))));
    }

    // ─────────────────────────── Moderación (PATCH /estado) ───────────────────────────

    @Test
    void operadorVerificaAvistamiento_cambiaEstadoYPersisteComentario() throws Exception {
        String id = crearAvistamiento();
        var body = Map.of("nuevoEstado", "VERIFICADO", "comentariosAdmin", "confirmado por cámara");

        mvc.perform(patch("/api/avistamientos/" + id + "/estado")
                        .header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VERIFICADO"))
                .andExpect(jsonPath("$.comentariosAdmin").value("confirmado por cámara"));

        // Persistió: una nueva lectura devuelve el estado y el comentario.
        mvc.perform(get("/api/avistamientos/" + id).header("Authorization", "Bearer " + tokenEstandar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VERIFICADO"))
                .andExpect(jsonPath("$.comentariosAdmin").value("confirmado por cámara"));
    }

    @Test
    void estandarNoPuedeCambiarEstado_da403() throws Exception {
        String id = crearAvistamiento();
        var body = Map.of("nuevoEstado", "VERIFICADO");

        mvc.perform(patch("/api/avistamientos/" + id + "/estado")
                        .header("Authorization", "Bearer " + tokenEstandar)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarEstadoAvistamientoInexistenteDa404() throws Exception {
        var body = Map.of("nuevoEstado", "DESCARTADO");

        mvc.perform(patch("/api/avistamientos/no-existe-9999/estado")
                        .header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
