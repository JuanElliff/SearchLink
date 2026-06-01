# Database Design — SearchLink

Modelo de datos vigente del backend SearchLink (TP Integrador, Ingeniería de Datos II, UADE).

Este documento describe la versión **post-Paso 1** del plan en `ESTADO.md §D3`: se volvió a un modelo de **3 colecciones** con `dispositivos` embebidos en `usuarios`. Reemplaza al `DATABASE_DESIGN.md` previo a nivel raíz, que documentaba el modelo intermedio de 4 colecciones.

---

## 1. Diagrama de colecciones

```
┌─────────────────────────────────────────────────────────────────────┐
│ usuarios                                                            │
├─────────────────────────────────────────────────────────────────────┤
│ _id              ObjectId                                           │
│ nombre           String                                             │
│ email            String           (unique)                          │
│ password_hash    String                                             │
│ rol              String           ("ciudadano" | "operador")        │
│ ubicacion_precargada  GeoJSON Point    (2dsphere normal — siempre)  │
│ ubicacion_actual      GeoJSON Point    (2dsphere sparse — opcional) │
│ activo           Boolean                                            │
│ dispositivos[]   ── Array de sub-objetos (embebidos)                │
│   ├─ fcm_token     String         (unique sparse — global)          │
│   ├─ plataforma    String         ("web" | "android" | "ios")       │
│   ├─ activo        Boolean        (toggle manual del usuario)       │
│   ├─ ultimo_uso    ISODate                                          │
│   └─ registrado_en ISODate                                          │
│ creado_en        ISODate                                            │
│ actualizado_en   ISODate                                            │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ alertas                                                             │
├─────────────────────────────────────────────────────────────────────┤
│ _id              ObjectId                                           │
│ menor            { nombre, edad, descripcion, foto_url, ... }       │
│ ubicacion        GeoJSON Point    (2dsphere)                        │
│ radio_km         Double                                             │
│ estado           String           ("ACTIVA"|"RESUELTA"|"CANCELADA") │
│ creada_por       ObjectId (ref usuarios)                            │
│ creada_en        ISODate                                            │
│ actualizada_en   ISODate                                            │
│ expira_en        ISODate          (TTL — auto-delete al vencer)     │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │  alerta_id
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│ avistamientos                                                       │
├─────────────────────────────────────────────────────────────────────┤
│ _id              ObjectId                                           │
│ alerta_id        ObjectId (ref alertas)                             │
│ reportado_por    ObjectId (ref usuarios)                            │
│ ubicacion        GeoJSON Point    (2dsphere)                        │
│ descripcion      String                                             │
│ foto_url         String           (opcional)                        │
│ verificado       Boolean                                            │
│ creado_en        ISODate                                            │
└─────────────────────────────────────────────────────────────────────┘
```

### Índices

| Colección | Índice | Tipo | Propósito |
|---|---|---|---|
| `usuarios` | `ubicacion_precargada` | 2dsphere | `$nearSphere` para despacho push (siempre presente — fuente actual de la query) |
| `usuarios` | `ubicacion_actual` | 2dsphere sparse | GPS opcional con consentimiento; sparse porque puede no existir en el documento |
| `usuarios` | `email` | unique | login / unicidad de cuenta |
| `usuarios` | `dispositivos.fcm_token` | unique sparse | un token FCM nunca pertenece a dos usuarios |
| `alertas` | `ubicacion` | 2dsphere | (futuro) búsquedas por área |
| `alertas` | `estado` | b-tree | listado de activas |
| `alertas` | `expira_en` | TTL | auto-borrado al vencer |
| `alertas` | `creada_en` | -1 | orden cronológico inverso |
| `avistamientos` | `ubicacion` | 2dsphere | mapear avistamientos |
| `avistamientos` | `alerta_id` | b-tree | listar avistamientos por alerta |
| `avistamientos` | `creado_en` | -1 | orden cronológico inverso |

---

## 2. Justificación del embedding de `dispositivos`

El array `dispositivos` se **embebe** en `usuarios` en lugar de vivir como una colección separada `dispositivos` referenciada por `usuario_id`. Tres razones, en orden de peso:

### 2.1. El path crítico del MVP es 1 query, no 2

El flujo "publicar alerta → notificar push a usuarios cercanos" es la operación crítica del sistema (criterio de aceptación slide 11: *"Push verificable"*). Comparación:

| Modelo | Operación de despacho push |
|---|---|
| 4 colecciones (separado) | `$nearSphere` sobre `usuarios` → lista de IDs → `findByUsuarioIdInAndActivoTrue(...)` sobre `dispositivos` → tokens. **2 queries.** |
| 3 colecciones (embebido) | `$nearSphere` sobre `usuarios` (proyectando `dispositivos.fcm_token`) → tokens directo. **1 query.** |

La proyección embebida elimina el segundo lookup. En `AlertaService.notificarUsuariosCercanos` (`backend/src/main/java/ar/edu/uade/searchlink/service/AlertaService.java`) los `usuarios` retornados por el `$nearSphere` ya traen sus `dispositivos`; el dispatch FCM (Paso 3 del plan) los consume directamente.

### 2.2. Array acotado por dominio

Un usuario en este dominio tiene típicamente **1 a 3 dispositivos** (móvil personal + opcional tablet + opcional sesión web). No hay riesgo realista de crecimiento unbounded ni de aproximarse al límite de 16 MB del documento. La regla "no embebas arrays que pueden crecer sin techo" no aplica.

### 2.3. Operaciones de mantenimiento siguen siendo atómicas

El argumento principal a favor de separar — "update granular sin reescribir el documento del usuario" — es marketing, no realidad operativa. MongoDB soporta operadores atómicos sobre arrays embebidos:

- **Registrar dispositivo:** `$addToSet` o `$push` sobre `dispositivos`.
- **Invalidar token muerto** (respuesta `UNREGISTERED` / `NotRegistered` de FCM): `$pull` por `fcm_token`. **Sin soft-delete.**
- **Marcar inactivo** (el usuario deshabilita notificaciones desde la UI): `$set` sobre `dispositivos.$[d].activo = false` con `arrayFilters`.
- **Actualizar `ultimo_uso`:** `$set` sobre `dispositivos.$[d].ultimo_uso` con `arrayFilters`.

Cada una es una operación atómica de un solo documento — no escala peor que una operación equivalente sobre una colección separada, y en el path de lectura escala mejor.

### 2.4. Política de invalidación de tokens

- **Token muerto** (FCM responde `UNREGISTERED` / `NotRegistered` en el dispatch): `$pull` del sub-objeto. Se elimina, no se conserva.
- **Desactivación manual** (el usuario apaga notificaciones): `activo: false`. Permanece en el array; se filtra en el dispatch.

El campo `activo` existe **únicamente** para el segundo caso. No se usa como "tombstone" para tokens muertos.

---

## 3. Modelo de ubicación del usuario (opción B: dos campos)

`usuarios` tiene **dos** campos de ubicación, no uno. Refleja que la posición que conocemos de un usuario puede venir de dos fuentes distintas con propiedades distintas.

### 3.1. Campos

| Campo | Tipo | ¿Siempre presente? | Origen | Índice |
|---|---|---|---|---|
| `ubicacion_precargada` | GeoJSON Point | **Sí** — obligatorio en registro | Carga el usuario al registrarse (domicilio, área de referencia) | `2dsphere` normal |
| `ubicacion_actual` | GeoJSON Point | No — opcional | GPS del dispositivo, sólo si el usuario consintió compartirlo (vía PWA) | `2dsphere` **sparse** |

El "consentimiento de GPS" no se modela como un boolean separado: se infiere por la presencia o ausencia de `ubicacion_actual`. Si el usuario otorgó permiso, el frontend reporta el GPS y el campo se llena; si lo revocó, el frontend hace `$unset` y el campo desaparece del documento. **No hay falsos negativos** por usuarios que olvidaron actualizar un flag.

### 3.2. Por qué `sparse` en `ubicacion_actual`

Los índices `2dsphere` ya ignoran documentos sin el campo, así que declarar `sparse: true` es estrictamente redundante a nivel comportamiento de MongoDB. Lo declaramos igual por dos razones:

1. **Documentación intencional:** marca explícitamente que `ubicacion_actual` es opcional. Sin esa declaración, lector futuro asumiría que el campo debe existir.
2. **Defensa contra cambios de tipo de índice:** si en el futuro alguien re-tipifica el índice (p. ej. compuesto con otro campo no-geo), `sparse` deja de ser inferible y el comportamiento "ignorar documentos sin el campo" sí se vuelve relevante.

### 3.3. Regla de coalesce en el despacho push

**Por cada usuario candidato, la ubicación efectiva para la query de proximidad es `coalesce(ubicacion_actual, ubicacion_precargada)`:** si el usuario tiene GPS reciente, se usa ese; si no, se cae al domicilio.

Justificación: un usuario que viajó al otro lado del país y compartió GPS no debería recibir push de alertas de su barrio de origen — debería recibir las del lugar donde está ahora. Recíprocamente, un usuario que nunca compartió GPS sigue siendo notificable usando su domicilio precargado.

### 3.4. Estado de implementación actual

`AlertaService.notificarUsuariosCercanos` corre `$nearSphere` **sólo contra `ubicacion_precargada`** en esta fase. Es la rama "domicilio" del coalesce; la rama "GPS" queda postergada al bloque de geolocalización (ver §3.5).

### 3.5. **Decisión pendiente:** estrategia para combinar las dos queries geo

El coalesce por-usuario no se puede expresar limpiamente como una sola query `$nearSphere`, porque MongoDB necesita un único índice geoespacial por operación de proximidad. Hay dos caminos abiertos, a resolver en el bloque de geolocalización:

**Opción 1 — Dos queries + unión de IDs sin duplicados.**

```text
Q1 = $nearSphere sobre ubicacion_actual (radio R)
Q2 = $nearSphere sobre ubicacion_precargada (radio R) AND ubicacion_actual no existe
Resultado = Q1 ∪ Q2 (deduplicado por _id)
```

Q2 excluye explícitamente a los usuarios con GPS para que ganen sólo a través de Q1 (su ubicación efectiva es la actual, no la precargada). Ventaja: simple, cada query usa su índice nativo, lectura cuasi-óptima. Desventaja: dos round-trips al servidor; la deduplicación se hace en el backend.

**Opción 2 — Campo efectivo materializado.**

Mantener un tercer campo `ubicacion_efectiva` denormalizado, computado en el backend cada vez que cambia `ubicacion_actual` o `ubicacion_precargada` (`$set` post-update). El push corre `$nearSphere` sobre `ubicacion_efectiva` en una sola query. Ventaja: una query, un índice. Desventaja: invariante de denormalización que hay que mantener — toda escritura de cualquiera de los dos campos debe disparar el recálculo, y un bug ahí causa que el push consulte data stale.

**Posicionamiento provisorio:** la Opción 1 es más simple operativamente y no introduce invariantes adicionales; la Opción 2 paga complejidad de escritura para ahorrar un round-trip de lectura. A decidir cuando exista el endpoint de actualización de GPS (bloque de geolocalización).

---

## 4. Divergencia documentada respecto al slide 8

### Lo que muestra el slide

El slide 8 de `SearchLink_Presentacion 1.pptx` modela los tokens así (extracto):

```
usuarios: {
  ...,
  fcm_tokens: [String]
}
```

Un array plano de strings. Sin metadata por token.

### Lo que se implementó

```
usuarios: {
  ...,
  dispositivos: [{
    fcm_token:     String,
    plataforma:    String,    // "web" | "android" | "ios"
    activo:        Boolean,
    ultimo_uso:    ISODate,
    registrado_en: ISODate
  }]
}
```

Sub-objetos con metadata por dispositivo.

### Justificación del ajuste (ítem 8 de la rúbrica — "ajustes justificados sobre el diseño original")

Tres motivos operativos que el array plano no soporta:

1. **Invalidación de tokens muertos.** Cuando FCM responde `UNREGISTERED` para un token, se necesita identificar *cuál* entrada del array remover. Con `[String]` plano se puede hacer (`$pull` por valor), pero se pierde la oportunidad de auditar qué dispositivo era. Con sub-objetos se mantiene el `plataforma` y `registrado_en` hasta el momento del pull, lo que permite (en logs / métricas) saber qué tipo de cliente fallaba.
2. **Diagnóstico de pérdida de engagement.** `ultimo_uso` por token permite distinguir un dispositivo activo de uno que el usuario abandonó. Esto es útil para futuras políticas (p. ej. dejar de intentar enviar a un token sin actividad en N meses).
3. **Toggle de notificaciones desde la UI sin perder el token.** El usuario puede desactivar push sin que el token se borre; cuando reactiva, no hay que re-registrar el device. Con `[String]` plano, el único camino es borrar y re-pedir el token al cliente.

Ninguno de estos requisitos contradice el slide a nivel conceptual — el slide modela "el usuario tiene varios tokens FCM" — sólo lo refina con la metadata operativa que el path real necesita. **El slide no se rehace** (es la presentación entregada); el ajuste se documenta acá como justificación del ítem 8 de la rúbrica.

---

## 5. Estado actual y próximos pasos

- **Implementado:** modelo de 3 colecciones; dos campos de ubicación (precargada normal + actual sparse); índices, script de init (`mongo/init/01_init.js`) con seeds que cubren ambas ramas del coalesce; lectura de tokens embebidos lista en `AlertaService` (variable `tokensActivos`).
- **Pendiente:**
  - Endpoints de registro de usuario, registro de dispositivo (`POST /api/usuarios/{id}/dispositivos`), actualización de `ubicacion_actual`, creación de avistamientos (Paso 2 del plan).
  - Dispatch FCM real con Firebase Admin SDK (Paso 3).
  - Resolver la decisión §3.5 (estrategia de combinación de queries geo) cuando arranque el bloque de geolocalización.

La sección de Unidad IV (replicación, CAP, consistencia) se agregará en el Paso 6 del plan.
