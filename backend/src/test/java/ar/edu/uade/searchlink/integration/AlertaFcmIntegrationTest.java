package ar.edu.uade.searchlink.integration;

import ar.edu.uade.searchlink.dto.RegistroUsuarioRequest;
import ar.edu.uade.searchlink.model.Alerta;
import ar.edu.uade.searchlink.model.RolUsuario;
import ar.edu.uade.searchlink.model.Usuario;
import ar.edu.uade.searchlink.repository.AlertaRepository;
import ar.edu.uade.searchlink.repository.UsuarioRepository;
import ar.edu.uade.searchlink.service.FcmService;
import ar.edu.uade.searchlink.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del dispatch FCM al crear una alerta, contra Mongo embebido y con FcmService
 * mockeado. Verifica que: (1) la query geoespacial alimenta a FcmService EXACTAMENTE con los tokens
 * activos de los usuarios cercanos; (2) si el envío falla, la alerta igual se crea (no 500); (3) los
 * tokens muertos devueltos se depuran de los usuarios vía $pull.
 *
 * NOTA: el $nearSphere necesita un índice 2dsphere sobre ubicacion_precargada. En tests lo crea el
 * auto-index-creation (habilitado en src/test/resources/application.properties); en producción está
 * apagado y el índice lo arma mongo/init/01_init.js. Por eso acá NO se crea a mano.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AlertaFcmIntegrationTest {

    private static final double LAT = -34.6037;
    private static final double LNG = -58.3816;

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

    @MockBean
    private FcmService fcmService;

    private String tokenOperador;

    @BeforeEach
    void setUp() throws Exception {
        usuarioRepository.deleteAll();
        alertaRepository.deleteAll();
        // El índice 2dsphere sobre ubicacion_precargada lo crea el auto-index-creation de Spring
        // Data al iniciar el contexto; no hace falta crearlo acá (hacerlo da IndexOptionsConflict).

        usuarioService.registrarOperador(
                new RegistroUsuarioRequest("Op", "op@x.com", "password123", LAT, LNG));
        tokenOperador = login("op@x.com", "password123");
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

    private Usuario.Dispositivo dispositivo(String fcmToken, boolean activo) {
        Date ahora = new Date();
        return Usuario.Dispositivo.builder()
                .fcmToken(fcmToken).plataforma("ANDROID").activo(activo)
                .registradoEn(ahora).ultimoUso(ahora).build();
    }

    /** Siembra un usuario ESTANDAR con ubicación y dispositivos dados, directo por repositorio. */
    private Usuario sembrarUsuario(String email, double lat, double lng, Usuario.Dispositivo... dispositivos) {
        Date ahora = new Date();
        Usuario u = Usuario.builder()
                .nombre(email).email(email).passwordHash("x").rol(RolUsuario.ESTANDAR).activo(true)
                .ubicacionPrecargada(new GeoJsonPoint(lng, lat)) // [lng, lat]
                .dispositivos(new ArrayList<>(List.of(dispositivos)))
                .creadoEn(ahora).actualizadoEn(ahora)
                .build();
        return usuarioRepository.save(u);
    }

    private void crearAlerta() throws Exception {
        var body = Map.of(
                "nombreMenor", "Juan Niño",
                "ubicacion", Map.of("latitud", LAT, "longitud", LNG),
                "radioKm", 10.0);
        mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated());
    }

    // ─────────────────────────── Tests ───────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void crear_pasaAFcmExactamenteLosTokensActivosCercanos() throws Exception {
        when(fcmService.enviarAlerta(any(), any())).thenReturn(List.of());

        sembrarUsuario("a@x.com", LAT, LNG, dispositivo("tk-A", true));
        sembrarUsuario("b@x.com", LAT, LNG, dispositivo("tk-B", true));
        sembrarUsuario("lejos@x.com", 0.0, 0.0, dispositivo("tk-lejos", true)); // fuera del radio
        sembrarUsuario("inactivo@x.com", LAT, LNG, dispositivo("tk-inactivo", false)); // device inactivo

        crearAlerta();

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(fcmService).enviarAlerta(captor.capture(), any(Alerta.class));
        assertThat(captor.getValue()).containsExactlyInAnyOrder("tk-A", "tk-B");
    }

    @Test
    void crear_siFcmLanza_laAlertaIgualSeCrea() throws Exception {
        when(fcmService.enviarAlerta(any(), any())).thenThrow(new RuntimeException("boom"));
        sembrarUsuario("a@x.com", LAT, LNG, dispositivo("tk-A", true));

        crearAlerta(); // sigue siendo 201 pese a la excepción del dispatch

        assertThat(alertaRepository.count()).isEqualTo(1);
    }

    @Test
    void crear_depuraLosTokensMuertosDevueltosPorFcm() throws Exception {
        when(fcmService.enviarAlerta(any(), any())).thenReturn(List.of("tk-dead"));
        sembrarUsuario("a@x.com", LAT, LNG, dispositivo("tk-dead", true), dispositivo("tk-live", true));

        crearAlerta();

        List<String> tokens = usuarioRepository.findByEmail("a@x.com").orElseThrow()
                .getDispositivos().stream().map(Usuario.Dispositivo::getFcmToken).toList();
        assertThat(tokens).containsExactly("tk-live"); // tk-dead fue depurado por $pull
    }
}
