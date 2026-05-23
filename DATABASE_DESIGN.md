# Database Design — SearchLink

Modelado de colecciones, índices y decisiones de diseño para el TP Integrador de Ingeniería de Datos II — UADE.

---

## Tabla de contenidos

1. [Colecciones](#colecciones)
   - [usuarios](#1-usuarios)
   - [dispositivos](#2-dispositivos)
   - [alertas](#3-alertas)
   - [avistamientos](#4-avistamientos)
2. [Redis como capa de caché](#redis-como-capa-de-caché)
3. [MongoDB vs alternativas](#mongodb-vs-alternativas)

---

## Colecciones

### 1. usuarios

Almacena los ciudadanos y operadores registrados en la plataforma.

#### Documento de ejemplo

```json
{
  "_id": { "$oid": "664a1f2e3b5c9d0012345678" },
  "nombre": "María González",
  "email": "maria.gonzalez@gmail.com",
  "password_hash": "$2b$10$eImiTXuWVxfM37uY9JVvEuTJkS1e...",
  "rol": "CIUDADANO",
  "ubicacion": {
    "type": "Point",
    "coordinates": [-58.3816, -34.6037]
  },
  "activo": true,
  "creado_en": { "$date": "2024-05-20T10:00:00Z" },
  "actualizado_en": { "$date": "2024-05-20T10:00:00Z" }
}
```

#### Campos

| Campo | Tipo | Descripción |
|---|---|---|
| `_id` | ObjectId | Identificador único generado por MongoDB |
| `nombre` | String | Nombre completo del usuario |
| `email` | String | Email único, usado como credencial de acceso |
| `password_hash` | String | Hash bcrypt de la contraseña (nunca texto plano) |
| `rol` | String (enum) | `CIUDADANO`, `OPERADOR` o `ADMIN` |
| `ubicacion` | GeoJSON Point | Coordenadas declaradas manualmente por el usuario |
| `activo` | Boolean | Permite deshabilitar usuarios sin eliminarlos |
| `creado_en` | Date | Timestamp de registro |
| `actualizado_en` | Date | Timestamp de última modificación |

#### Decisiones de diseño

- **Ubicación como GeoJSON Point:** el campo `ubicacion` sigue el estándar GeoJSON (`{ type: "Point", coordinates: [lng, lat] }`). Esta estructura es obligatoria para que MongoDB aplique el índice `2dsphere` y habilite las consultas `$nearSphere` al momento de publicar una alerta.
- **Ubicación declarativa, no rastreada:** el usuario define su ubicación manualmente al registrarse. No se registra la posición en tiempo real. Decisión de privacidad: el sistema no necesita saber dónde está el ciudadano en cada instante, sino en qué radio geográfico desea recibir alertas.
- **Password como hash, no texto plano:** se almacena el hash bcrypt. La contraseña original nunca toca la base de datos.
- **Campo `activo` en lugar de borrado físico:** permite deshabilitar cuentas sin perder el historial del usuario ni romper referencias en otras colecciones.

#### Índices

| Campo | Tipo | Justificación |
|---|---|---|
| `ubicacion` | `2dsphere` | Habilita consultas geoespaciales eficientes al publicar alertas |
| `email` | Único | Impide duplicados y acelera el login |

---

### 2. dispositivos

Almacena los tokens FCM de los dispositivos registrados por cada usuario para recibir notificaciones push.

#### Documento de ejemplo

```json
{
  "_id": { "$oid": "664a2c3d4e5f6a0023456789" },
  "usuario_id": { "$oid": "664a1f2e3b5c9d0012345678" },
  "fcm_token": "fME3x9kT2Rs:APA91bH...",
  "plataforma": "ANDROID",
  "activo": true,
  "registrado_en": { "$date": "2024-05-20T10:05:00Z" },
  "ultimo_uso": { "$date": "2024-06-01T08:30:00Z" }
}
```

#### Campos

| Campo | Tipo | Descripción |
|---|---|---|
| `_id` | ObjectId | Identificador único del dispositivo |
| `usuario_id` | ObjectId | Referencia al documento en la colección `usuarios` |
| `fcm_token` | String | Token de Firebase Cloud Messaging para notificaciones push |
| `plataforma` | String (enum) | `ANDROID`, `IOS` o `WEB` |
| `activo` | Boolean | Permite invalidar tokens sin eliminarlos |
| `registrado_en` | Date | Cuándo fue registrado el dispositivo |
| `ultimo_uso` | Date | Última vez que el token fue utilizado con éxito |

#### Decisiones de diseño

- **Colección separada de usuarios:** un usuario puede tener múltiples dispositivos (teléfono, tablet, computadora con portal web). Almacenar los tokens como array dentro del documento de usuario introduciría arrays de tamaño variable y dificultaría la invalidación individual de tokens sin reescribir el documento completo.
- **Tokens FCM como documentos independientes:** los tokens cambian frecuentemente (reinstalación de app, rotación de token por Firebase). Tenerlos como documentos propios permite actualizarlos o desactivarlos sin tocar el documento del usuario.
- **Campo `ultimo_uso`:** permite implementar una limpieza periódica de tokens inactivos (tokens sin uso por más de 90 días probablemente ya no son válidos).

#### Índices

| Campo | Tipo | Justificación |
|---|---|---|
| `usuario_id` | Normal | Buscar todos los dispositivos de un usuario al despachar notificaciones |
| `fcm_token` | Único | Impide registrar el mismo token dos veces |

---

### 3. alertas

Almacena las alertas publicadas sobre niños desaparecidos.

#### Documento de ejemplo

```json
{
  "_id": { "$oid": "664b3e4f5a6b7c0034567890" },
  "nombre_menor": "Lucas Rodríguez",
  "edad": 8,
  "descripcion": "Ropa azul, mochila verde. Visto por última vez en Plaza Italia.",
  "foto_url": "https://storage.searchlink.dev/fotos/lucas-rodriguez.jpg",
  "ubicacion": {
    "type": "Point",
    "coordinates": [-58.4241, -34.5881]
  },
  "radio_km": 10,
  "estado": "ACTIVA",
  "creada_por": { "$oid": "664a1f2e3b5c9d0012345678" },
  "creada_en": { "$date": "2024-06-05T14:23:00Z" },
  "expira_en": { "$date": "2024-06-12T14:23:00Z" },
  "actualizada_en": { "$date": "2024-06-05T14:23:00Z" }
}
```

#### Campos

| Campo | Tipo | Descripción |
|---|---|---|
| `_id` | ObjectId | Identificador único de la alerta |
| `nombre_menor` | String | Nombre del niño desaparecido |
| `edad` | Integer | Edad en años |
| `descripcion` | String | Descripción física y contexto de la desaparición |
| `foto_url` | String | URL de la foto almacenada en el servicio de archivos |
| `ubicacion` | GeoJSON Point | Lugar donde ocurrió o se reportó la desaparición |
| `radio_km` | Double | Radio en kilómetros para notificar a usuarios cercanos |
| `estado` | String (enum) | `ACTIVA`, `RESUELTA` o `CANCELADA` |
| `creada_por` | ObjectId | Referencia al operador que publicó la alerta |
| `creada_en` | Date | Timestamp de creación |
| `expira_en` | Date | Fecha de expiración — MongoDB elimina el documento automáticamente |
| `actualizada_en` | Date | Timestamp de última modificación |

#### Decisiones de diseño

- **Alerta como documento autocontenido:** toda la información necesaria para mostrar la alerta está en un único documento. No se requieren joins con otras colecciones para renderizar la ficha de una alerta. Esto reduce la latencia de lectura en el escenario más frecuente.
- **Índice TTL sobre `expira_en`:** MongoDB elimina el documento automáticamente cuando el campo `expira_en` alcanza la fecha actual. Esto evita la necesidad de un proceso batch externo para limpiar alertas vencidas. El valor `expireAfterSeconds: 0` le indica a MongoDB que use el valor del campo como timestamp de expiración exacto.
- **Índice sobre `estado`:** la consulta más frecuente es `findByEstado(ACTIVA)`. Sin índice, MongoDB haría un collection scan sobre todas las alertas (incluidas las resueltas y canceladas) en cada solicitud del portal.
- **Índice `2dsphere` sobre `ubicacion`:** permite la consulta `$nearSphere` para encontrar alertas cercanas a un punto dado. También habilita mostrar alertas en un mapa dentro de un radio visible.

#### Índices

| Campo | Tipo | Justificación |
|---|---|---|
| `ubicacion` | `2dsphere` | Consultas de proximidad (`$nearSphere`, `$geoWithin`) |
| `estado` | Normal | Filtrar alertas activas eficientemente |
| `expira_en` | TTL (`expireAfterSeconds: 0`) | Eliminación automática de alertas vencidas |
| `creada_en` | Normal (desc) | Orden cronológico inverso en el listado general |

---

### 4. avistamientos

Almacena los reportes de avistamiento enviados por ciudadanos en respuesta a una alerta activa.

#### Documento de ejemplo

```json
{
  "_id": { "$oid": "664c4f5a6b7c8d0045678901" },
  "alerta_id": { "$oid": "664b3e4f5a6b7c0034567890" },
  "reportado_por": { "$oid": "664a1f2e3b5c9d0012345678" },
  "ubicacion": {
    "type": "Point",
    "coordinates": [-58.4310, -34.5920]
  },
  "descripcion": "Vi a un niño con esas características en la esquina de Corrientes y Medrano, hace 10 minutos.",
  "foto_url": "https://storage.searchlink.dev/avistamientos/foto-20240605.jpg",
  "creado_en": { "$date": "2024-06-05T15:10:00Z" },
  "verificado": false
}
```

#### Campos

| Campo | Tipo | Descripción |
|---|---|---|
| `_id` | ObjectId | Identificador único del avistamiento |
| `alerta_id` | ObjectId | Referencia a la alerta que originó el reporte |
| `reportado_por` | ObjectId | Referencia al usuario que envió el avistamiento |
| `ubicacion` | GeoJSON Point | Lugar donde el ciudadano dice haber visto al menor |
| `descripcion` | String | Descripción libre del avistamiento |
| `foto_url` | String | Foto opcional capturada en el momento |
| `creado_en` | Date | Timestamp del reporte |
| `verificado` | Boolean | Si fue validado por un operador o fuerza de seguridad |

#### Decisiones de diseño

- **Colección separada, no array dentro de alertas:** una alerta puede recibir decenas o cientos de avistamientos. Almacenarlos como array embebido dentro del documento de alerta haría crecer ese documento indefinidamente, acercándose al límite de 16 MB de MongoDB y degradando la performance de lectura de la alerta (se cargarían todos los avistamientos aunque no se necesiten).
- **Referencia a alerta por `alerta_id`:** permite consultar todos los avistamientos de una alerta paginando eficientemente, sin cargar el documento principal de la alerta.
- **Campo `verificado`:** en una iteración futura, los operadores pueden marcar avistamientos como verificados o descartados. El campo está presente desde el inicio para no requerir una migración de esquema posterior.
- **Ubicación propia del avistamiento:** la ubicación del avistamiento puede diferir de la ubicación de la alerta (el menor pudo haberse desplazado). Tenerla como campo propio permite mapear la trayectoria de avistamientos.

#### Índices

| Campo | Tipo | Justificación |
|---|---|---|
| `ubicacion` | `2dsphere` | Mapear avistamientos en el portal y calcular distancia al punto de la alerta |
| `alerta_id` | Normal | Obtener todos los avistamientos de una alerta eficientemente |
| `creado_en` | Normal (desc) | Orden cronológico inverso en el panel de operadores |

---

## Redis como capa de caché

> **Estado: mejora futura — no forma parte del MVP.**

### Problema que resuelve

Las alertas activas son el recurso más consultado del sistema: el portal web las refresca periódicamente, los usuarios móviles las solicitan al abrir la app y los operadores las revisan en tiempo real. Sin caché, cada una de estas lecturas ejecuta una consulta a MongoDB con filtro por `estado = ACTIVA`.

Durante un pico de actividad (ej.: una alerta de alto impacto mediático), esta colección puede recibir cientos de lecturas por segundo, con resultados que no cambian entre consulta y consulta.

### Diseño propuesto

```
Cliente → Backend → ¿Existe clave en Redis? ──Sí──► Devuelve respuesta cacheada
                          │
                          No
                          │
                          ▼
                       MongoDB (consulta real)
                          │
                          ▼
                     Guarda en Redis con TTL de 60 segundos
                          │
                          ▼
                     Devuelve respuesta al cliente
```

- **Clave Redis:** `alertas:activas`
- **Valor:** JSON serializado de la lista de alertas activas
- **TTL:** 60 segundos. Pasado ese tiempo, la próxima lectura va a MongoDB y actualiza la caché.
- **Invalidación activa:** cuando se publica, resuelve o cancela una alerta, el backend elimina la clave `alertas:activas` de Redis para forzar una recarga inmediata.

### Justificación de la exclusión del MVP

| Criterio | Evaluación |
|---|---|
| Complejidad operativa | Requiere un cuarto servicio (Redis) en Docker Compose |
| Valor en el MVP | Con el volumen esperado del TP (decenas de requests), MongoDB responde en < 5 ms |
| Consistencia | Introduce una ventana de inconsistencia de hasta 60 segundos |
| Mantenimiento | Requiere gestionar invalidación de caché correctamente |

Redis se diseña como mejora para una versión de producción con volumen real, no como requisito del TP.

---

## MongoDB vs alternativas

### Tabla comparativa

| Criterio | **MongoDB** | **PostgreSQL** | **Cassandra** |
|---|---|---|---|
| Modelo de datos | Documentos JSON flexibles | Tablas relacionales con esquema rígido | Columnar distribuido |
| Consultas geoespaciales | Nativo (`2dsphere`, `$nearSphere`) | Extensión PostGIS (potente pero externa) | No nativo |
| Esquema | Flexible — evoluciona sin migraciones | Rígido — cada cambio requiere `ALTER TABLE` | Semi-flexible |
| Operación principal | Lectura/escritura de documentos completos | Queries con múltiples joins | Alta tasa de escritura distribuida |
| Escalado | Horizontal (sharding) | Vertical principalmente | Horizontal nativo |
| Joins | No nativo (`$lookup` disponible pero costoso) | Nativo y eficiente | No |
| TTL automático | Sí (índice TTL nativo) | No (requiere cron job externo) | Sí (TTL por columna) |
| Curva de aprendizaje | Media | Alta (SQL + modelo relacional) | Alta (modelo de datos distinto) |

### Por qué MongoDB para SearchLink

**1. Consultas geoespaciales nativas**
El núcleo del sistema es la consulta `$nearSphere`: al publicar una alerta, buscar todos los usuarios dentro de un radio en milisegundos. MongoDB tiene este soporte integrado con el índice `2dsphere`. Con PostgreSQL requeriría instalar y configurar PostGIS; con Cassandra no existiría sin una capa personalizada.

**2. Documento de alerta autocontenido**
Una alerta concentra todos sus datos en un único documento (foto, descripción, ubicación, estado, timestamps). No hay joins necesarios para mostrar la ficha de una alerta. Esto es un ajuste natural al modelo documental de MongoDB y sería un anti-patrón en un modelo relacional.

**3. TTL automático**
El índice TTL de MongoDB elimina alertas vencidas sin proceso externo. En PostgreSQL habría que implementar un cron job o una tarea programada. En un TP universitario, el TTL nativo elimina una pieza de infraestructura que no aporta valor académico.

**4. Esquema evolutivo**
Durante el desarrollo del TP el esquema cambia con frecuencia (se agregan campos, se renombran, se reestructuran arrays). Con MongoDB estos cambios son locales: no hay `ALTER TABLE`, no hay migraciones de esquema, no hay downtime. Los documentos anteriores conviven con los nuevos sin conflicto.

**5. Alineación con el contenido de la materia**
La materia es Bases de Datos NoSQL — Arquitectura y Modelado. Usar MongoDB permite aplicar directamente los conceptos del curso: modelado de documentos, índices especializados, estrategias de embedding vs referencia y consultas de agregación.