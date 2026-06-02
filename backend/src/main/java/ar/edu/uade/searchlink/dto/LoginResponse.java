package ar.edu.uade.searchlink.dto;

/** Respuesta de login: el JWT más los datos básicos del usuario autenticado. */
public record LoginResponse(
        String token,
        String tokenTipo,
        UsuarioResponse usuario
) {
    public static LoginResponse de(String token, UsuarioResponse usuario) {
        return new LoginResponse(token, "Bearer", usuario);
    }
}
