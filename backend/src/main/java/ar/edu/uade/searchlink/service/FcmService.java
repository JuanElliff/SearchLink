package ar.edu.uade.searchlink.service;

import ar.edu.uade.searchlink.model.Alerta;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Envío de notificaciones push vía Firebase Cloud Messaging.
 *
 * Tolerante a la ausencia de credencial: si FirebaseApp no fue inicializado (ver FirebaseConfig)
 * el envío es un no-op. NUNCA propaga excepciones: ante cualquier error devuelve lista vacía, para
 * no romper el flujo de creación de alerta que lo invoca.
 */
@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private static final String TITULO = "SearchLink — alerta cercana";
    private static final int MAX_TOKENS_POR_LOTE = 500;

    /**
     * Envía la alerta como push multicast a los tokens dados.
     *
     * @return los tokens MUERTOS (error UNREGISTERED) detectados, para que el llamador los depure.
     *         Lista vacía si FCM está deshabilitado, no hay tokens, o ante cualquier error.
     */
    public List<String> enviarAlerta(List<String> tokens, Alerta alerta) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.warn("FCM deshabilitado: no se envía push (FirebaseApp no inicializado)");
                return List.of();
            }
            if (tokens == null || tokens.isEmpty()) {
                log.info("Sin tokens activos: no se envía push (alerta {})", idDe(alerta));
                return List.of();
            }

            String alertaId = idDe(alerta);
            String body = construirCuerpo(alerta);

            List<String> muertos = new ArrayList<>();
            int totalOk = 0;
            int totalFallidos = 0;

            for (int offset = 0; offset < tokens.size(); offset += MAX_TOKENS_POR_LOTE) {
                List<String> lote = tokens.subList(offset, Math.min(offset + MAX_TOKENS_POR_LOTE, tokens.size()));
                MulticastMessage mensaje = MulticastMessage.builder()
                        .addAllTokens(lote)
                        .putData("title", TITULO)
                        .putData("body", body)
                        .putData("alertaId", alertaId)
                        .build();
                BatchResponse respuesta = FirebaseMessaging.getInstance().sendEachForMulticast(mensaje);
                muertos.addAll(recolectarTokensMuertos(lote, respuesta));
                totalOk += respuesta.getSuccessCount();
                totalFallidos += respuesta.getFailureCount();
            }

            log.info("Push alerta {}: {} ok, {} fallidos, {} tokens muertos a depurar",
                    alertaId, totalOk, totalFallidos, muertos.size());
            return muertos;
        } catch (Exception e) {
            log.error("Error enviando push FCM para la alerta {}", idDe(alerta), e);
            return List.of();
        }
    }

    /** Cuerpo enriquecido SÓLO con campos existentes del modelo Alerta (nombreMenor). */
    private String construirCuerpo(Alerta alerta) {
        String nombre = alerta != null ? alerta.getNombreMenor() : null;
        return (nombre != null && !nombre.isBlank())
                ? "Se busca a " + nombre + " cerca de tu zona. Tocá para ver el detalle."
                : "Hay una alerta activa cerca de tu zona. Tocá para ver el detalle.";
    }

    /**
     * Recorre el BatchResponse en paralelo a la lista de tokens y junta los que fallaron con
     * MessagingErrorCode.UNREGISTERED (tokens muertos). Null-guard sobre getException() antes de
     * getMessagingErrorCode(): si una respuesta fallida no trae código claro, NO se marca como
     * muerta (sólo UNREGISTERED se depura; INVALID_ARGUMENT u otros se ignoran acá).
     */
    private List<String> recolectarTokensMuertos(List<String> tokens, BatchResponse respuesta) {
        List<String> muertos = new ArrayList<>();
        List<SendResponse> respuestas = respuesta.getResponses();
        int n = Math.min(respuestas.size(), tokens.size());
        for (int i = 0; i < n; i++) {
            SendResponse r = respuestas.get(i);
            if (r.isSuccessful()) {
                continue;
            }
            FirebaseMessagingException ex = r.getException();
            if (ex == null) {
                continue; // fallo sin excepción/código claro → no se asume muerto
            }
            if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                muertos.add(tokens.get(i));
            }
        }
        return muertos;
    }

    private String idDe(Alerta alerta) {
        return alerta != null && alerta.getId() != null ? alerta.getId() : "";
    }
}
