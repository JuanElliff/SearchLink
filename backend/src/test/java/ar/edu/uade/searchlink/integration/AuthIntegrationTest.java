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

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración de auth + matriz de roles, contra MongoDB embebido.
 * Ejercita la cadena real: filtro JWT + SecurityFilterChain + @PreAuthorize + controllers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    // Alerta válida para CrearAlertaRequest: ubicacion como {latitud, longitud}.
    private static final String ALERTA_JSON =
            "{\"nombreMenor\":\"Menor Test\",\"edad\":8,\"descripcion\":\"desc\","
            + "\"ubicacion\":{\"latitud\":-34.6037,\"longitud\":-58.3816},"
            + "\"radioKm\":10.0}";

    @BeforeEach
    void limpiar() {
        usuarioRepository.deleteAll();
    }

    private String json(Object o) throws Exception {
        return om.writeValueAsString(o);
    }

    /** Body de registro con ubicación obligatoria incluida. */
    private String registro(String nombre, String email, String password) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre);
        m.put("email", email);
        m.put("password", password);
        m.put("latitud", -34.6037);
        m.put("longitud", -58.3816);
        return json(m);
    }

    private RegistroUsuarioRequest req(String nombre, String email) {
        return new RegistroUsuarioRequest(nombre, email, "password123", -34.6037, -58.3816);
    }

    private String registrarYObtenerId(String nombre, String email, String password) throws Exception {
        String resp = mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(registro(nombre, email, password)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("id").asText();
    }

    private void desactivar(String email) {
        Usuario u = usuarioRepository.findByEmail(email).orElseThrow();
        u.setActivo(false);
        usuarioRepository.save(u);
    }

    private String loginYObtenerToken(String email, String password) throws Exception {
        String resp = mvc.perform(post("/api/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("token").asText();
    }

    // ─────────────────────────── Flujo base ───────────────────────────

    @Test
    void flujoCompleto_registroLoginAccesoProtegidoYDenegaciones() throws Exception {
        // 1. Registro público → 201, rol ESTANDAR, sin passwordHash en la respuesta.
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Juan", "juan@x.com", "password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ESTANDAR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        // 2. Login → 200 con token.
        String token = loginYObtenerToken("juan@x.com", "password123");

        // 3. Endpoint protegido CON token → 200.
        mvc.perform(get("/api/alertas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 4. El mismo endpoint SIN token → 401.
        mvc.perform(get("/api/alertas"))
                .andExpect(status().isUnauthorized());

        // 5. Token inválido/malformado → 401 (NO 500).
        mvc.perform(get("/api/alertas").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConCredencialesInvalidasDa401() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Juan", "juan@x.com", "password123")))
                .andExpect(status().isCreated());

        // Password incorrecto → 401.
        mvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "juan@x.com", "password", "incorrecta"))))
                .andExpect(status().isUnauthorized());

        // Email inexistente → 401 (mismo trato, no se revela cuál falló).
        mvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nadie@x.com", "password", "password123"))))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────── Validación ───────────────────────────

    @Test
    void registroInvalidoDa400ConCamposListados() throws Exception {
        // email mal formado + password corto.
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "Juan", "email", "no-es-email",
                                "password", "corta", "latitud", -34.6, "longitud", -58.3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].campo").exists());
    }

    @Test
    void registroSinUbicacionDa400() throws Exception {
        // Falta latitud/longitud → ubicación obligatoria incumplida.
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "Juan", "email", "juan@x.com",
                                "password", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registroConEmailDuplicadoDa409() throws Exception {
        String body = registro("Juan", "dup@x.com", "password123");
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    // ─────────────────── Matriz de roles: gestión de usuarios ───────────────────

    @Test
    void soloAdminPuedeCrearOtroAdmin() throws Exception {
        usuarioService.registrarAdmin(req("Jefe", "jefe@x.com"));
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Juan", "juan@x.com", "password123")))
                .andExpect(status().isCreated());

        String tokenAdmin = loginYObtenerToken("jefe@x.com", "password123");
        String tokenEstandar = loginYObtenerToken("juan@x.com", "password123");
        String nuevoAdmin = registro("Nuevo", "nuevo-admin@x.com", "password123");

        // Estándar NO puede → 403.
        mvc.perform(post("/api/usuarios/admin").header("Authorization", "Bearer " + tokenEstandar)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevoAdmin))
                .andExpect(status().isForbidden());

        // Admin SÍ puede → 201 y el creado es ADMIN.
        mvc.perform(post("/api/usuarios/admin").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevoAdmin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void soloAdminPuedeCrearOperador() throws Exception {
        usuarioService.registrarAdmin(req("Jefe", "jefe@x.com"));
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Juan", "juan@x.com", "password123")))
                .andExpect(status().isCreated());

        String tokenAdmin = loginYObtenerToken("jefe@x.com", "password123");
        String tokenEstandar = loginYObtenerToken("juan@x.com", "password123");
        String nuevoOperador = registro("Op", "nuevo-operador@x.com", "password123");

        // Un ESTANDAR NO puede crear operador → 403.
        mvc.perform(post("/api/usuarios/operador").header("Authorization", "Bearer " + tokenEstandar)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevoOperador))
                .andExpect(status().isForbidden());

        // Admin SÍ puede → 201 y el creado es OPERADOR.
        mvc.perform(post("/api/usuarios/operador").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevoOperador))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("OPERADOR"));
    }

    // ─────────────────── Matriz de roles: alertas (solo OPERADOR) ───────────────────

    @Test
    void operadorPuedeCrearAlerta() throws Exception {
        usuarioService.registrarOperador(req("Op", "op@x.com"));
        String tokenOperador = loginYObtenerToken("op@x.com", "password123");

        mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON).content(ALERTA_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void adminNoPuedeCrearAlerta() throws Exception {
        usuarioService.registrarAdmin(req("Jefe", "jefe@x.com"));
        String tokenAdmin = loginYObtenerToken("jefe@x.com", "password123");

        // Separación de responsabilidades: el ADMIN no opera alertas → 403.
        mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(ALERTA_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void estandarNoPuedeCrearAlerta() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Juan", "juan@x.com", "password123")))
                .andExpect(status().isCreated());
        String tokenEstandar = loginYObtenerToken("juan@x.com", "password123");

        mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenEstandar)
                        .contentType(MediaType.APPLICATION_JSON).content(ALERTA_JSON))
                .andExpect(status().isForbidden());
    }

    // ─────────────────── Agujero 1: cuenta desactivada (activo:false) ───────────────────

    @Test
    void usuarioInactivoNoPuedeLoguear() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Juan", "juan@x.com", "password123")))
                .andExpect(status().isCreated());
        desactivar("juan@x.com");

        // Credenciales correctas pero cuenta inactiva → 401 genérico (sin revelar la causa).
        mvc.perform(post("/api/sesiones").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "juan@x.com", "password", "password123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDesactivadoConTokenPrevioRecibe401() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Juan", "juan@x.com", "password123")))
                .andExpect(status().isCreated());
        String token = loginYObtenerToken("juan@x.com", "password123");

        // El token funciona mientras la cuenta está activa.
        mvc.perform(get("/api/alertas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Se desactiva la cuenta DESPUÉS de emitir el token: el token deja de valer → 401.
        desactivar("juan@x.com");
        mvc.perform(get("/api/alertas").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────── Agujero 2: privacidad de GET /api/usuarios/{id} ───────────────────

    @Test
    void estandarVeSuPropioPerfil() throws Exception {
        String id = registrarYObtenerId("Juan", "juan@x.com", "password123");
        String token = loginYObtenerToken("juan@x.com", "password123");

        mvc.perform(get("/api/usuarios/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@x.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void estandarNoVePerfilAjeno() throws Exception {
        String idOtro = registrarYObtenerId("Otro", "otro@x.com", "password123");
        registrarYObtenerId("Juan", "juan@x.com", "password123");
        String tokenJuan = loginYObtenerToken("juan@x.com", "password123");

        // Un ESTANDAR pidiendo el perfil de otro → 403.
        mvc.perform(get("/api/usuarios/" + idOtro).header("Authorization", "Bearer " + tokenJuan))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminYOperadorVenCualquierPerfil() throws Exception {
        String idObjetivo = registrarYObtenerId("Juan", "juan@x.com", "password123");
        usuarioService.registrarAdmin(req("Jefe", "jefe@x.com"));
        usuarioService.registrarOperador(req("Op", "op@x.com"));

        String tokenAdmin = loginYObtenerToken("jefe@x.com", "password123");
        String tokenOperador = loginYObtenerToken("op@x.com", "password123");

        mvc.perform(get("/api/usuarios/" + idObjetivo).header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/" + idObjetivo).header("Authorization", "Bearer " + tokenOperador))
                .andExpect(status().isOk());
    }

    // ─────────────────── Agujero 3: rango de lat/lng ───────────────────

    @Test
    void registroConLatitudFueraDeRangoDa400() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "Juan", "email", "juan@x.com",
                                "password", "password123", "latitud", 200.0, "longitud", -58.3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].campo").value("latitud"));
    }
}
