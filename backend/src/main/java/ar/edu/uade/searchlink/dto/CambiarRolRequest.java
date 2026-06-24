package ar.edu.uade.searchlink.dto;

import ar.edu.uade.searchlink.model.RolUsuario;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(
        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol
) {}
