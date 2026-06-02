package ar.edu.uade.searchlink.dto;

import ar.edu.uade.searchlink.model.RolUsuario;
import ar.edu.uade.searchlink.model.Usuario;

/**
 * DTO de salida para usuario. NO incluye passwordHash — el hash nunca sale de la API.
 */
public record UsuarioResponse(
        String id,
        String nombre,
        String email,
        RolUsuario rol,
        boolean activo
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.isActivo());
    }
}
