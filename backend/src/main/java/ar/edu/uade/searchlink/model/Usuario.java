package ar.edu.uade.searchlink.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    private String id;

    private String nombre;

    @Indexed(unique = true)
    private String email;

    @Field("password_hash")
    private String passwordHash;

    private RolUsuario rol;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    @Field("ubicacion_precargada")
    private GeoJsonPoint ubicacionPrecargada;

    // Sparse 2dsphere index defined en mongo/init/01_init.js — la anotacion @GeoSpatialIndexed
    // no expone el flag sparse y por defecto auto-index-creation esta apagado en Spring Boot 3.
    @Field("ubicacion_actual")
    private GeoJsonPoint ubicacionActual;

    private boolean activo;

    @Builder.Default
    private List<Dispositivo> dispositivos = new ArrayList<>();

    @Field("creado_en")
    private Date creadoEn;

    @Field("actualizado_en")
    private Date actualizadoEn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dispositivo {

        @Field("fcm_token")
        private String fcmToken;

        private String plataforma;

        private boolean activo;

        @Field("ultimo_uso")
        private Date ultimoUso;

        @Field("registrado_en")
        private Date registradoEn;
    }
}
