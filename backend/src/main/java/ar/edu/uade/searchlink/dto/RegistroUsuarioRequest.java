package ar.edu.uade.searchlink.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request de registro (público) y de alta de admin/operador.
 *
 * SEGURIDAD: deliberadamente NO tiene campo `rol`. El rol no es un dato que el cliente
 * pueda elegir — lo asigna el servidor (ESTANDAR en registro público, ADMIN/OPERADOR en
 * los endpoints protegidos). Sin el campo, un body malicioso no puede inyectar rol.
 *
 * `latitud`/`longitud` son OBLIGATORIAS: con ellas se construye server-side la
 * ubicacion_precargada, que según el diseño de datos está "siempre presente" y sostiene
 * el índice 2dsphere y la query de despacho push. Registro sin ubicación → 400.
 */
public record RegistroUsuarioRequest(

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        // @Min/@Max no aplican a Double (la spec los reserva para enteros por errores de
        // redondeo); para coordenadas se usan @DecimalMin/@DecimalMax.
        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
        Double longitud
) {
}
