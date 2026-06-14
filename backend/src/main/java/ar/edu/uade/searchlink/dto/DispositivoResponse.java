package ar.edu.uade.searchlink.dto;

import ar.edu.uade.searchlink.model.Usuario;

import java.util.Date;

/**
 * DTO de salida del dispositivo registrado. No expone el documento Usuario completo ni el propio
 * `fcmToken` (no hace falta devolverle al cliente el token que él mismo mandó). Sólo refleja el
 * estado del registro.
 */
public record DispositivoResponse(
        String plataforma,
        boolean activo,
        Date registradoEn,
        Date ultimoUso
) {
    public static DispositivoResponse from(Usuario.Dispositivo d) {
        return new DispositivoResponse(
                d.getPlataforma(), d.isActivo(), d.getRegistradoEn(), d.getUltimoUso());
    }
}
