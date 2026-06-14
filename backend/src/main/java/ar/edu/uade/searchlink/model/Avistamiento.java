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

import java.util.Date;

@Document(collection = "avistamientos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Avistamiento {

    @Id
    private String id;

    @Indexed
    @Field("alerta_id")
    private String alertaId;

    @Field("reportado_por")
    private String reportadoPor;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint ubicacion;

    private String descripcion;

    @Field("foto_url")
    private String fotoUrl;

    @Field("creado_en")
    private Date creadoEn;

    /** Estado de moderación. Nace PENDIENTE; sólo un OPERADOR lo cambia. */
    @Field("estado_verificacion")
    private EstadoVerificacion estado;

    /** Nota opcional del operador al verificar/descartar. Nullable. */
    @Field("comentarios_admin")
    private String comentariosAdmin;
}
