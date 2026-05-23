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

@Document(collection = "alertas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alerta {

    @Id
    private String id;

    @Field("nombre_menor")
    private String nombreMenor;

    private Integer edad;

    private String descripcion;

    @Field("foto_url")
    private String fotoUrl;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint ubicacion;

    @Field("radio_km")
    private Double radioKm;

    @Indexed
    private EstadoAlerta estado;

    @Field("creada_por")
    private String creadaPor;

    @Field("creada_en")
    private Date creadaEn;

    @Indexed(expireAfterSeconds = 0)
    @Field("expira_en")
    private Date expiraEn;

    @Field("actualizada_en")
    private Date actualizadaEn;
}
