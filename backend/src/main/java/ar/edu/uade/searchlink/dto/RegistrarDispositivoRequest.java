package ar.edu.uade.searchlink.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request de alta/actualización de token FCM del usuario autenticado.
 *
 * Deliberadamente NO incluye `userId`: el dueño del dispositivo es SIEMPRE el usuario del JWT
 * (Authentication.getName()), nunca un id que el cliente pueda mandar. `plataforma` es String
 * libre (ej. "ANDROID"/"IOS"/"WEB"); no se valida contra un enum por decisión del bloque.
 */
public record RegistrarDispositivoRequest(

        @NotBlank(message = "El token FCM es obligatorio")
        String fcmToken,

        @NotBlank(message = "La plataforma es obligatoria")
        String plataforma
) {
}
