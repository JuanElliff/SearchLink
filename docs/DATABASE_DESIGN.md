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
│ rol              String           ("ADMIN" | "OPERADOR" | "ESTANDAR")│
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

## 4. Divergencias documentadas respecto a la presentación (Entrega 1)

Verificado contra el contenido real de la presentación. Hay **tres** divergencias puntuales respecto al código, todas registradas acá como ajustes justificados (ítem 8 de la rúbrica — "ajustes justificados sobre el diseño original"). Las dos primeras son contra el slide 8 (modelo de datos); la tercera contra el slide 5 (roles).

### 4.1. Divergencia de NOMBRE: `perfiles_busqueda` → `alertas`

| Fuente | Nombre de la colección de la persona desaparecida |
|---|---|
| Entrega 1, slide 8 | `perfiles_busqueda` |
| Código implementado | `alertas` |

**Justificación.** La entidad implementada unifica *persona desaparecida* + *caso/aviso activo* en un único documento: no es sólo el perfil de la persona, es la alerta publicada con su estado, radio de despacho, vencimiento (TTL) y ubicación. El nombre `alertas` describe esa entidad operativa con más precisión que `perfiles_busqueda`. Se unifica toda la terminología del proyecto bajo "alerta".

**DEUDA.** El slide 8 de la **PRESENTACIÓN FINAL** debe rehacerse para decir `alertas`, no `perfiles_busqueda`. La Entrega 1 ya fue entregada y no se reescribe; la corrección aplica a la presentación final para que la documentación quede consistente con el código.

### 4.2. Divergencia de ESTRUCTURA de tokens: lista plana → sub-objetos

| Fuente | Modelo de tokens FCM |
|---|---|
| Entrega 1, slide 8 | `fcm_tokens` — lista plana de strings, sin estructura |
| Código implementado | `dispositivos[]` — array de sub-objetos `{ fcm_token, plataforma, activo, ultimo_uso, registrado_en }` |

**Justificación.** La lista plana no permite invalidar tokens caducados de forma segura. Cuando FCM responde `UNREGISTERED` para un token, el sistema hace `$pull` del sub-objeto correspondiente; el modelo de sub-objetos además conserva metadata (`plataforma`, `ultimo_uso`) útil para auditoría y habilita el toggle manual de notificaciones vía el flag `activo`. Es la práctica recomendada por la documentación de FCM para el ciclo de vida de tokens (registro → uso → invalidación al caducar).

En concreto, lo que la lista plana no soporta y los sub-objetos sí:

1. **Invalidación de tokens muertos con auditoría.** El `$pull` ante `UNREGISTERED` conserva `plataforma`/`registrado_en` hasta el momento del borrado, lo que permite (en logs / métricas) saber qué tipo de cliente fallaba.
2. **Diagnóstico de pérdida de engagement.** `ultimo_uso` por token distingue un dispositivo activo de uno abandonado, base para futuras políticas (p. ej. dejar de enviar a un token sin actividad en N meses).
3. **Toggle de notificaciones sin perder el token.** El usuario desactiva push (`activo: false`) sin que el token se borre; al reactivar no hay que re-registrar el device. Con lista plana el único camino es borrar y re-pedir el token al cliente.

Ninguna de estas dos divergencias contradice el slide 8 a nivel conceptual — el slide ya modela 3 colecciones y "el usuario tiene varios tokens FCM" — sólo refinan terminología (4.1) y estructura (4.2) según lo que el path real necesita.

### 4.3. Divergencia de ROLES: dos roles → tres roles

| Fuente | Roles del sistema |
|---|---|
| Entrega 1, slide 5 | dos roles: "administrador" y "estándar" |
| Código implementado | tres roles: `ADMIN`, `OPERADOR`, `ESTANDAR` |

**Justificación.** El modelo de dos roles colapsa dos responsabilidades que en el dominio son distintas y conviene separar:

- **ADMIN** — dueño/administrador de la *aplicación*: gestiona cuentas (da de alta operadores y otros admins) y modera avistamientos. **No emite alertas.**
- **OPERADOR** — autoridad *operativa* que emite y gestiona las alertas (el acto sensible del sistema). No administra cuentas.
- **ESTANDAR** — ciudadanía: reporta avistamientos y recibe push.

Separar ADMIN de OPERADOR aplica **separación de responsabilidades**: quien gobierna las cuentas no opera el sistema de alertas, y quien opera alertas no gobierna las cuentas. Esto reduce el daño si una credencial se ve comprometida (un ADMIN comprometido no puede disparar alertas falsas; un OPERADOR comprometido no puede crear cuentas privilegiadas) y deja una matriz de autorización auditable. La autorización efectiva vive en `SecurityConfig` (reglas por URL) reforzada con `@PreAuthorize` en los controllers.

**DEUDA.** El slide 5 de la **PRESENTACIÓN FINAL** debe reflejar los tres roles (`ADMIN`, `OPERADOR`, `ESTANDAR`) y la matriz de autorización asociada. La Entrega 1 no se reescribe.

---

## 5. Decisiones pendientes registradas

Decisiones abiertas que se resuelven en bloques posteriores; se registran acá para no perderlas.

### 5.1. Índice `unique: true` sobre `dispositivos.fcm_token` — A REVISAR

Hoy el init declara `db.usuarios.createIndex({ "dispositivos.fcm_token": 1 }, { unique: true, sparse: true })`.

**Riesgo:** los tokens FCM pueden **reasignarse entre usuarios** — p. ej. logout de un usuario y login de otro en el mismo dispositivo físico, donde FCM puede devolver el mismo token. El constraint `unique` bloquearía esa reasignación con error de duplicado en el alta del segundo usuario, en vez de transferir el token.

**A decidir cuando se implemente el alta de dispositivos** (`POST /api/usuarios/{id}/dispositivos`): si se mantiene `unique` y el flujo de alta hace `$pull` del token en cualquier otro usuario antes de insertarlo (garantizando unicidad por reasignación explícita), o si se relaja el constraint.

### 5.2. Query 2 (avistamientos por cercanía) — ¿devolver DISTANCIA?

El cambio de `geoNear` → `find` con `Criteria` en `AlertaService` dejó de exponer la distancia de cada resultado (sólo filtra por radio, no la devuelve).

**A decidir en el bloque de geolocalización:** si la query de avistamientos por cercanía necesita **ordenar por distancia** (y por lo tanto devolverla), en cuyo caso habría que volver a una agregación `$geoNear` que expone `distanceField`, o si alcanza con el filtrado por radio sin orden por cercanía.

### 5.3. Combinación de las dos queries geo (coalesce de ubicación)

Ver §3.5: dos queries + unión sin duplicados (Opción 1) vs. campo `ubicacion_efectiva` materializado (Opción 2). A resolver cuando exista el endpoint de actualización de GPS.

---

## 6. Decisiones de diseño cerradas (autenticación / autorización)

Decisiones tomadas y firmes (no son deuda ni quedan pendientes); se registran para defensa.

### 6.1. `ubicacion_precargada` obligatoria para TODOS los roles

El registro exige `ubicacion_precargada` también para ADMIN y OPERADOR, no sólo para la ciudadanía. Es a propósito: mantiene uniforme el modelo y sostiene el invariante **"todo usuario tiene ubicación"**, sobre el que descansan el índice `2dsphere` y la query de despacho push. Aceptar usuarios sin ubicación abriría una rama de nulos en el path crítico. El costo (pedirle coordenadas a una cuenta administrativa) es menor que romper el invariante.

### 6.2. Doble capa de autorización (URL + `@PreAuthorize`)

La autorización se declara en **dos lugares a propósito**: reglas por URL en `SecurityConfig` (capa de filtros) y `@PreAuthorize` en los métodos privilegiados de los controllers (capa de método). Es **defensa en profundidad**, no redundancia accidental: si un refactor de rutas mueve un endpoint y olvida su regla de URL, la anotación de método sigue cubriéndolo (y viceversa). Se asume el costo de mantener ambas en sincronía. **No simplificar a una sola capa.**

### 6.3. Verificación de cuenta activa en cada request

El filtro JWT carga el usuario por `_id` en cada request autenticada y rechaza si fue desactivado (`activo:false`) o borrado, aunque el token siga vigente. Como no hay refresh token ni blacklist en este alcance, este lookup es el mecanismo que hace que **desactivar una cuenta surta efecto inmediato**. El login también rechaza cuentas inactivas (401 genérico, sin revelar la causa).

---

## 7. Estado actual y próximos pasos

- **Implementado:** modelo de 3 colecciones; dos campos de ubicación (precargada normal + actual sparse); índices, script de init (`mongo/init/01_init.js`) con seeds que cubren ambas ramas del coalesce; lectura de tokens embebidos lista en `AlertaService` (variable `tokensActivos`).
- **Pendiente:**
  - Endpoints de registro de usuario, registro de dispositivo (`POST /api/usuarios/{id}/dispositivos`), actualización de `ubicacion_actual`, creación de avistamientos (Paso 2 del plan).
  - Dispatch FCM real con Firebase Admin SDK (Paso 3).
  - Resolver la decisión §3.5 (estrategia de combinación de queries geo) cuando arranque el bloque de geolocalización.

La sección de Unidad IV (replicación, CAP, consistencia) se agregará en el Paso 6 del plan.
