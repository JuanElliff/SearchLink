package ar.edu.uade.searchlink.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Document(collection = "dispositivos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispositivo {

    @Id
    private String id;

    @Field("usuario_id")
    private String usuarioId;

    @Indexed(unique = true)
    @Field("fcm_token")
    private String fcmToken;

    private Plataforma plataforma;

    private boolean activo;

    @Field("registrado_en")
    private Date registradoEn;

    @Field("ultimo_uso")
    private Date ultimoUso;
}
