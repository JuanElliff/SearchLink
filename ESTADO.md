# ESTADO — SearchLink

**Fecha de relevamiento:** 2026-05-22
**Baseline:** estado real del repo en `Projects/SearchLink/` al día de la fecha.
**Alcance del informe:** análisis de situación entre Parcial 1 (Diseño, 25%) y Parcial 2 (Implementación, 30%). El Final (45%) queda fuera de objetivo inmediato.

> Verificaciones no ejecutadas en este relevamiento: no hay `mvn` ni wrapper `mvnw` en el entorno, y el daemon de Docker no está corriendo. Por lo tanto, **no se verificó compilación ni arranque**. Todo lo etiquetado "NO VERIFICADO" abajo está derivado únicamente de lectura estática.

---

## Sesión 2026-06-15 — cierre

> Sección más reciente. Cierra el frontend completo (3 roles) y 3 micro-bloques de backend.
> Lo anterior (2026-06-14 en adelante) queda como referencia histórica.

### Log completo del repositorio (fuente de verdad)

```
7350432 feat(frontend): panel ADMIN — gestión de usuarios (lista, toggle activo, crear OPERADOR/ADMIN)
b0908f0 feat: gestión de usuarios ADMIN (listar + activar/desactivar con guard anti-lockout)
2ed6003 feat(frontend): OPERADOR moderación de avistamientos (mapa + verificar/descartar)
81d78e2 feat(frontend): panel OPERADOR — lista, crear y editar alertas
5820f24 feat(frontend): pantallas ESTANDAR (mapa, detalle, reportar avistamiento)
e74afc0 feat: GET /api/alertas/{id} (detalle por id, cualquier estado)
052baf8 feat(frontend): scaffolding + auth (Vite/React/Tailwind/Leaflet/PWA, routing por rol)
51b2452 feat: dispatch FCM real al crear alerta (Firebase Admin SDK, limpieza de tokens UNREGISTERED)
c9a4ed8 docs(estado): cerrar avistamientos + dispositivos + FCM, actualizar roadmap
0f2c15a feat: alta de token FCM (POST /api/dispositivos, upsert idempotente)
3b20366 feat: avistamientos CRUD + verificar/descartar (estado enum, authz OPERADOR)
205ed45 chore: ignorar .claude/ (config local de Claude Code)
e88b51d docs(estado): registrar cierre de sesión 2026-06-01 en ESTADO.md
ade23f7 Alertas: DTOs propios + creado_por server-side + DuplicateKey 409
2ecbf7f Auth: JWT + bcrypt + 3 roles (ADMIN/OPERADOR/ESTANDAR)
a6887c5 Docs: cerrar pendientes del Paso 1
b109f42 Refactor modelo: 3 colecciones + ubicacion precargada/actual
6129edb Initial commit: SearchLink backend skeleton + project docs
```

### Completado y commiteado esta sesión

**Backend — micro-bloques post-FCM:**

- **`GET /api/alertas/{id}` — detalle por id, cualquier estado.** Commit **`e74afc0`**.
  - Devuelve la alerta sin filtrar por estado (`ACTIVA`/`RESUELTA`/`CANCELADA`): el deep-link del push
    FCM debe poder abrirla aunque ya no esté activa.
  - Authz: `GET /api/alertas/**` ya estaba `.authenticated()` en `SecurityConfig`; no se tocó.
  - Tests: existente 200 + campos, alerta RESUELTA sigue 200, inexistente 404, sin token 401.

- **`GET /api/usuarios` — listar todos (ADMIN-only).** Commit **`b0908f0`**.
  - Reutiliza `UsuarioResponse` (id, nombre, email, rol, activo). Sin paginación (escala demo).
  - `@PreAuthorize("hasRole('ADMIN')")`.

- **`PATCH /api/usuarios/{id}/activo` — toggle activo (ADMIN-only).** Commit **`b0908f0`**.
  - Body `{ "activo": Boolean }` (boxed + `@NotNull`). Devuelve `UsuarioResponse` actualizado.
  - **Guard anti-lockout:** un ADMIN no puede desactivarse a sí mismo → `OperacionInvalidaException`
    → 400 uniforme via `GlobalExceptionHandler` (excepción + handler nuevos).
  - 404 si el usuario no existe. Tests: listar como ADMIN 200, OPERADOR/ESTANDAR 403,
    desactivar otro 200, no-ADMIN 403, id inexistente 404, auto-desactivación 400.
  - **Suite total backend: 57 tests verdes.**

**Frontend — Vite 8 / React 19 / Tailwind 3 / react-leaflet 5 / vite-plugin-pwa:**

> Construido contra el contrato del backend (DTOs/enums verificados en código Java).
> **NUNCA se corrió contra el backend real** (docker compose no estaba levantado durante el
> desarrollo). Pendiente: verificación en vivo (ver Roadmap).

- **Scaffold + auth.** Commit **`052baf8`**.
  - `AuthContext`: login `POST /api/sesiones`, guarda `{ token, usuario }` en `localStorage`.
    Rol sale del `LoginResponse.usuario.rol` (sin llamada extra).
  - `api/client.js`: wrapper fetch con JWT automático. **Auto-logout en 401** solo si el request
    llevaba `Authorization` (no dispara en login/registro).
  - `ProtectedRoute`: sin sesión → `/login`; rol no permitido → home del propio rol.
  - `LocationPicker`: Leaflet, pin por click o GPS.
  - PWA: manifest + service worker (instalable).

- **ESTANDAR.** Commit **`5820f24`**.
  - Mapa de alertas activas (`GET /api/alertas`). GeoJSON `[lng,lat]` invertido a `[lat,lng]` para
    Leaflet en todos los mapas.
  - Detalle `/alerta/:id` (`GET /api/alertas/{id}`): info + mapa con `Circle` de radioKm. Botón
    "Reportar avistamiento" solo si `estado === 'ACTIVA'`.
  - Reportar `/alerta/:id/avistamiento`: `POST /api/avistamientos` con ubicación **anidada**
    `{latitud, longitud}` y `fotoUrl: null` (upload diferido).

- **OPERADOR-A.** Commit **`81d78e2`**.
  - Lista de alertas activas con "Crear" y "Editar" por item.
  - Crear: `POST /api/alertas`, ubicación **anidada**, `edad` → `parseInt` o `null`.
  - Editar `/operador/alerta/:id/editar`: solo `estado` y `radioKm` (lo único que acepta
    `ActualizarAlertaRequest`), pre-rellenos desde `GET /api/alertas/{id}`.

- **OPERADOR-B.** Commit **`2ed6003`**.
  - Moderación `/operador/alerta/:id/avistamientos`: fetch paralelo alerta + avistamientos.
    Mapa con marker+Circle de alerta y markers por avistamiento (null-guard sobre `ubicacion`).
    Lista con badge `EstadoVerificacion`, acciones "Verificar"/"Descartar" para PENDIENTE.
    `PATCH /api/avistamientos/{id}/estado` con `{nuevoEstado, comentariosAdmin}`. Nunca manda
    `PENDIENTE` (rechazado por `@AssertTrue` del backend). Actualización local sin re-fetch.

- **ADMIN.** Commits **`b0908f0`** (backend) + **`7350432`** (frontend).
  - Lista `GET /api/usuarios`, badges de rol/activo.
  - Toggle activo → `PATCH /api/usuarios/{id}/activo { activo: !actual }`. Actualización local.
    Guard en UI: botón deshabilitado cuando `esPropioAdmin && u.activo` (replica el guard del
    backend). Si llega un 400, muestra el mensaje inline.
  - Crear usuario `/admin/crear`: selector OPERADOR|ADMIN, body **plano** `{nombre, email,
    password, latitud, longitud}` (distinto de alertas/avistamientos que van anidados).

**Decisiones de contrato frontend → backend (puntos de atención para la verificación en vivo):**
- GeoJSON `coordinates = [lng, lat]` → se invierte a `[lat, lng]` para Leaflet en todos los mapas.
- Body de ubicación: **plano** en `POST /api/usuarios` y `POST /api/usuarios/{operador,admin}` (top-level `latitud`/`longitud`); **anidado** `{ubicacion:{latitud,longitud}}` en alertas y avistamientos.
- `fotoUrl` en alertas y avistamientos: URL string (no upload). Upload diferido a un bloque posterior.
- `edad` en alertas: `parseInt(string, 10)` o `null` (nunca string vacío).

### Estado del árbol

`main` con **12 commits no pusheados** (desde `3b20366`). Working tree limpio. **57 tests
backend verdes. Build frontend OK (446 kB JS + PWA). Lint limpio.** Verificación E2E: pendiente.

### Roadmap restante (orden de prioridad)

1. **VERIFICACIÓN EN VIVO** — `docker compose up --build` + login real + flujo completo de cada rol.
   **Prioridad máxima**: es el primer momento en que el frontend toca el backend real. Cualquier
   divergencia de contrato (CORS, encoding, campos inesperados) se descubre aquí.
2. **FCM en el frontend** — registrar token FCM del browser (`POST /api/dispositivos`) al hacer
   login. Requiere `firebaseConfig` del proyecto Firebase y una VAPID key para el SW.
3. **Docs**: OpenAPI/Swagger (`springdoc-openapi`), reescritura del `README.md`, manual de usuario.
4. **Unidad IV en papel** (CAP / consistencia / replicación en `DATABASE_DESIGN.md`).
5. **E2E + preparación de la defensa final.**

---

## Sesión 2026-06-14 — cierre

> Sección de retoma más reciente. Cierra tres bloques desde el corte del 2026-06-01. Lo de
> abajo (## Sesión 2026-06-01 en adelante) queda como referencia histórica.

### Completado y commiteado

- **Avistamientos: bloque cerrado.** CRUD + moderación. Commit **`3b20366`**.
  - **Migración del modelo:** el campo `verificado` (boolean) se reemplazó por `estado` (enum
    nuevo `EstadoVerificacion` { PENDIENTE, VERIFICADO, DESCARTADO }, `@Field("estado_verificacion")`,
    nace PENDIENTE server-side) + nuevo campo `comentariosAdmin` (`@Field("comentarios_admin")`, nullable).
  - **Endpoints:** `POST /api/avistamientos` (cualquier autenticado; `reportadoPor` server-side desde
    el token, no del body), `GET /api/avistamientos?alertaId=`, `GET /api/avistamientos/{id}` (404 si no
    existe), `PATCH /api/avistamientos/{id}/estado` (solo OPERADOR). Alerta inexistente al reportar → 404.
  - El **PATCH rechaza volver a PENDIENTE** vía `@AssertTrue` en el DTO → 400 uniforme del
    `GlobalExceptionHandler` (solo admite VERIFICADO/DESCARTADO).
  - **Authz activada en `SecurityConfig`:** se reemplazó la regla pendiente comentada por
    `PATCH /api/avistamientos/*/estado` → `hasRole("OPERADOR")`. POST y GET quedan `.authenticated()`.
  - DTOs propios (`CrearAvistamientoRequest` / `AvistamientoResponse` con ubicación GeoJSON /
    `CambiarEstadoAvistamientoRequest`); reutiliza `UbicacionRequest` (lat/lng con `@DecimalMin/@DecimalMax`).
  - 6 tests de integración (flapdoodle) verdes.

- **Dispositivos: bloque cerrado.** Alta/refresh de token FCM. Commit **`0f2c15a`**.
  - `POST /api/dispositivos` (**path PLANO, sin `{id}`**): el dueño es SIEMPRE el usuario del JWT
    (`Authentication.getName()` == userId, mismo mecanismo que `creado_por` en Alertas). El cliente
    nunca manda `userId`. Devuelve **200** (alta o actualización), no 201.
  - **Upsert idempotente por `fcmToken`** dentro de la lista embebida `Usuario.Dispositivo` (no se
    modela colección aparte): si el token ya está → refresca `ultimoUso` + `activo=true`, sin duplicar;
    si no → agrega `{fcmToken, plataforma, activo, registradoEn, ultimoUso}`.
  - **Guard de lista null** sobre `getDispositivos()` (puede venir null al materializar desde Mongo,
    mismo guard que `AlertaService`).
  - DTOs propios (`RegistrarDispositivoRequest` con `fcmToken`/`plataforma` `@NotBlank`, plataforma
    String libre sin enum; `DispositivoResponse` chico que **no echa el token**).
  - **No se tocó `SecurityConfig`:** cae en `anyRequest().authenticated()` (cualquier autenticado
    registra su propio token); sin regla redundante.
  - 5 tests de integración verdes.

- **FCM real: dispatch implementado y verificado.** `firebase-admin` 9.9.0. Push real al publicar una
  alerta. *(Código en el working tree, todavía SIN commitear; este `ESTADO.md` se commitea aparte.)*
  - **FirebaseConfig** (`@PostConstruct`): inicializa `FirebaseApp` UNA vez si `firebase.credentials.path`
    apunta a un archivo existente (log INFO "FCM habilitado"); si está vacía o el archivo no existe, log
    WARN y la app **arranca igual** (tolerante: no rompe boot ni tests).
  - **FcmService**: `sendEachForMulticast` (`sendMulticast` quedó deprecado en 9.9.0, verificado contra
    el jar). No-op si no hay credencial (FirebaseApp no inicializado) o no hay tokens. `try/catch` total:
    NUNCA propaga. Devuelve los tokens **UNREGISTERED** (muertos), con null-guard de `getException()`
    antes de `getMessagingErrorCode()` (solo UNREGISTERED se depura; INVALID_ARGUMENT no).
  - **AlertaService**: reemplazó el `// TODO`. Tras el envío, depura los tokens muertos con un solo
    `$pull` (`updateMulti`) sobre `dispositivos.fcm_token` de TODOS los usuarios. Dispatch **SÍNCRONO**
    dentro de `crear()`, envuelto en `try/catch` para que un fallo de FCM no rompa la creación de la
    alerta (ya guardada antes del dispatch).
  - **Credencial** por env var `FIREBASE_CREDENTIALS_PATH` (propiedad `firebase.credentials.path`); el
    `serviceAccountKey.json` está **gitignoreado** (`*serviceAccountKey*.json` / `firebase-*.json`),
    fuera del repo.
  - **Tests:** 5 nuevos (2 unit de `FcmService` sin credencial; 3 de integración `AlertaFcmIntegrationTest`
    con `FcmService` mockeado: tokens exactos / no-500 ante excepción / `$pull` de muertos). **Suite total:
    46 verdes**; los **41 previos verdes SIN credencial** → tolerancia confirmada.
  - **Aprendizaje:** en tests `auto-index-creation=true` (`src/test/resources`), el 2dsphere de
    `ubicacion_precargada` se **auto-crea** → NO crearlo a mano en el test (causaba `IndexOptionsConflict`,
    error 85).

### Estado del árbol

`main` **2 commits de código adelante de `origin/main`, sin push**: `3b20366` (avistamientos) →
`0f2c15a` (dispositivos). El **bloque FCM está implementado y verificado (46 tests verdes) pero su
commit de código queda pendiente en el working tree**; este `ESTADO.md` se commitea por separado.
**Suite total: 46 tests verdes.**

### Roadmap restante (orden actualizado)

> **FCM real (Firebase Admin SDK): CERRADO** esta sesión (ver arriba). Ya no está bloqueado.

1. **Frontend** (React / Vite / Tailwind / Leaflet / PWA).
2. **Docs**: OpenAPI/Swagger, reescritura del README, manual de usuario.
3. **Unidad IV en papel** (CAP / consistencia / replicación).
4. **Integración E2E + preparación de la defensa final.**

### Deuda nueva / verificaciones de esta sesión

- **Seguridad del registro y roles — VERIFICADO YA CERRADO (no es deuda abierta).** Se planteó como
  posible deuda que `POST /api/usuarios` debería forzar ESTANDAR y que habría riesgo de escalada si el
  registro aceptara un `rol` arbitrario. **El código ya lo previene:** `RegistroUsuarioRequest` no tiene
  campo `rol` (un body malicioso no puede inyectarlo), `UsuarioService.registrar()` hardcodea
  `RolUsuario.ESTANDAR`, y la creación de OPERADOR/ADMIN va por `POST /api/usuarios/{operador,admin}`
  protegidos con `@PreAuthorize("hasRole('ADMIN')")` (cubierto por `AuthIntegrationTest`). Cerrado en
  el commit `2ecbf7f`. Queda registrado como verificación, no como pendiente.

> Deudas heredadas que siguen abiertas (ver sección 2026-06-01 §"Deudas registradas"): índice
> `unique sparse` sobre `dispositivos.fcm_token` (deuda explícitamente fuera del scope del bloque de
> dispositivos), reasignación de token entre usuarios, distancia en query de avistamientos, slide 8 de
> la presentación final, recuperación de contraseña, combinación de las 2 queries geo.

---

## Sesión 2026-06-01 — cierre

> Sección de retoma. Lo de abajo (## 0 en adelante) es el relevamiento original del 2026-05-22 y queda como referencia histórica; varios puntos ya se resolvieron (ver acá).

### Completado y commiteado

- **Modelo: 3 colecciones** (`alertas`, `avistamientos`, `usuarios`) con `dispositivos` embebidos en `usuarios`; `ubicacion_precargada` (2dsphere normal) + `ubicacion_actual` (2dsphere sparse). Commit **`b109f42`**.
- **Docs del Paso 1** (divergencias vs slide 8 + decisiones pendientes). Commit **`a6887c5`**.
- **Auth completo:** JWT HS256 24h + bcrypt cost 10 + **3 roles** (ADMIN/OPERADOR/ESTANDAR). Registro público fuerza ESTANDAR (hueco de privilegios tapado), chequeo de `activo` en login y en el filtro JWT, `GET /api/usuarios/{id}` restringido por propiedad/rol, validación, errores uniformes. **24 tests** verdes (Mongo embebido flapdoodle) al momento del commit. Commit **`2ecbf7f`**.
- **Refactor de alertas a DTOs propios** (`CrearAlertaRequest` / `ActualizarAlertaRequest` / `AlertaResponse`), `creado_por` desde el `SecurityContext` (no falsificable), `estado`/`creada_en`/`expira_en` server-side, validación ubicación/radio/lat-lng, `DuplicateKeyException → 409`. Commit **`ade23f7`**. (Este bloque YA se ejecutó; no es "en curso".) Suite total actual: **30 tests** verdes.

### Estado del árbol

`main` 4 commits adelante de `origin/main`, **sin push**. Working tree limpio. Orden de commits: `6129edb → b109f42 → a6887c5 → 2ecbf7f → ade23f7`.

### Próximo al retomar

El bloque de alertas-DTO ya está cerrado. **El próximo bloque es avistamientos.** Dos huecos chicos quedaron señalados en la autocrítica del bloque de alertas, para resolver al arrancar lo próximo:
- No hay test del `PATCH /api/alertas/{id}` (actualizar estado/radio) — cobertura faltante.
- Divergencia consciente: el request de alerta usa `ubicacion: {latitud, longitud}` (para validar rango con `@DecimalMin/@DecimalMax`), no GeoJSON `{type, coordinates}`; la **respuesta** sí es GeoJSON estándar. Revisar si se mantiene.

### Orden de bloques restantes

avistamientos (CRUD + verificar; **descomentar la regla de authz ya comentada en `SecurityConfig`**: verificar = OPERADOR+ADMIN)
→ dispositivos (alta de token FCM)
→ FCM real (**CREAR proyecto Firebase = bloqueante**)
→ frontend (React/Vite/Tailwind/Leaflet, PWA)
→ docs/OpenAPI/manual/README
→ Unidad IV en papel (CAP/consistencia)
→ integración E2E + presentación final.

### Deudas registradas

- Índice `unique sparse` en `dispositivos.fcm_token`: revisar por reasignación de tokens (logout/login en mismo device). Ver `docs/DATABASE_DESIGN.md §5.1`.
- Query de avistamientos por cercanía: ver si necesita devolver distancia (el cambio `geoNear → find` la dejó de exponer). `docs/DATABASE_DESIGN.md §5.2`.
- Presentación **FINAL**: rehacer slide 8 (colección `alertas`, no `perfiles_busqueda`) y slide 5 (3 roles, no 2). `docs/DATABASE_DESIGN.md §4`.
- Recuperación de contraseña por email: deuda, al final si hay tiempo.
- Combinación de las 2 queries geo (coalesce `ubicacion_actual`/`ubicacion_precargada`): decidir en el bloque de geolocalización. `docs/DATABASE_DESIGN.md §3.5 / §5.3`.
- `DuplicateKeyException → 409` está mapeado y testeado unitariamente, pero `UsuarioService.registrar` sigue con pre-check `findByEmail`; para cerrar la carrera de verdad faltaría dejar propagar la colisión del índice al handler.

### Decisiones descartadas (no reabrir sin motivo)

Refresh token, blacklist de logout, rate limiting, caché del lookup de usuario en el filtro JWT.

### Cómo retomar

Próximo paso: **arrancar el bloque de avistamientos** (CRUD + verificar; descomentar la regla authz). El refactor de alertas a DTOs ya está hecho (`ade23f7`).

**Seeds QA** (passwords en claro, del init `mongo/init/01_init.js` — *corregidos respecto al prompt de cierre, que tenía la credencial del operador desactualizada*):

| Email | Password | Rol |
|---|---|---|
| `admin@searchlink.dev` | `Admin1234` | ADMIN |
| `operador@searchlink.dev` | `Operador1234` | OPERADOR |
| `belgrano@searchlink.dev` | `Estandar1234` | ESTANDAR |
| `caballito@searchlink.dev` | `Estandar1234` | ESTANDAR |

**Build/test:** no hay `mvn` en PATH; se usa el Maven cacheado del wrapper (`~/.m2/wrapper/dists/apache-maven-3.9.14/.../bin/mvn -f backend/pom.xml clean test`). Docker no es necesario para los tests (Mongo embebido flapdoodle descarga un `mongod` local).

---

## 0. Hallazgos cruzados antes del inventario

Tres desalineaciones de fondo que afectan todo lo demás:

1. **`README.md` describe una arquitectura distinta a la del código.** El README dice "Node.js + Express", "Replica Set MongoDB de 3 nodos", `backend/.env.example`, paso de `rs.initiate` con tres miembros. El código real es **Java 21 + Spring Boot 3.3.4**, `docker-compose.yml` levanta **un único nodo Mongo** (no replica set), y `backend/.env.example` **no existe**. El README está desactualizado respecto al código. Evidencia: `README.md:95-128, 188-214` vs `backend/pom.xml:9-39`, `docker-compose.yml:3-13`.
2. **El modelado entregado en la Presentación 1 no es el implementado.** El slide 8 lista tres colecciones (`perfiles_busqueda`, `avistamientos`, `usuarios`) con `persona` embebida y `fcm_tokens` como array dentro de `usuarios`. El código tiene cuatro colecciones (`alertas`, `avistamientos`, `usuarios`, `dispositivos`) con `dispositivos` separada. Es un cambio razonable (y mejor justificado en `DATABASE_DESIGN.md`), pero **no está documentado como un ajuste sobre el diseño original** — está oculto. Esto es exactamente el ítem 8 de la rúbrica.
3. **No existe "presentación final" en el repo.** En `../Presentaciones/` hay un único archivo: `SearchLink_Presentacion 1.pptx`. La "última diapositiva del final" que se pidió leer es la diapositiva 11 de esa única presentación: "Entregables y criterios de aceptación". Los entregables del enunciado original también aparecen en `SearchLink TPO.docx §4`. Los uso como contrato vigente.

---

## 1. Inventario real — declarado vs. funciona

| Módulo | Declarado (archivo/símbolo existe) | Funciona de verdad | Evidencia |
|---|---|---|---|
| Esqueleto Spring Boot | Sí, `@SpringBootApplication` arranca por default | NO VERIFICADO (sin `mvn`/Docker en el entorno) | `backend/src/main/java/ar/edu/uade/searchlink/SearchLinkApplication.java:7-11` |
| Conexión a MongoDB | Sí, URI por env var | NO VERIFICADO; configuración correcta a primera vista | `backend/src/main/resources/application.properties:4`, `backend/src/main/java/ar/edu/uade/searchlink/config/MongoConfig.java:10-17` |
| Modelo `Alerta` | Sí, con `@GeoSpatialIndexed` y TTL declarativo | Mapea OK, debería persistir bien | `backend/src/main/java/ar/edu/uade/searchlink/model/Alerta.java:22-58` |
| Modelo `Usuario` | Sí, con `2dsphere` y email único | Mapea OK | `backend/src/main/java/ar/edu/uade/searchlink/model/Usuario.java:22-47` |
| Modelo `Dispositivo` | Sí, con `fcm_token` único | Mapea OK | `backend/src/main/java/ar/edu/uade/searchlink/model/Dispositivo.java:19-40` |
| Modelo `Avistamiento` | Sí, con `2dsphere` | Mapea OK | `backend/src/main/java/ar/edu/uade/searchlink/model/Avistamiento.java:22-46` |
| Repositorios (4) | Sí, los cuatro definidos como `MongoRepository<>` | Métodos derivados estándar; deberían funcionar | `backend/src/main/java/ar/edu/uade/searchlink/repository/*.java` |
| Endpoint crear alerta | `POST /api/alertas` declarado | NO VERIFICADO; sin validación, sin DTO, recibe `Alerta` cruda con `@RequestBody` (cliente podría setear `id`, `estado`, `creadaPor`, etc.) | `backend/src/main/java/ar/edu/uade/searchlink/controller/AlertaController.java:20-23`, `backend/src/main/java/ar/edu/uade/searchlink/service/AlertaService.java:29-36` |
| Endpoint listar activas | `GET /api/alertas` declarado | NO VERIFICADO; sin paginación | `AlertaController.java:25-28`, `AlertaService.java:38-40` |
| Endpoint cambiar estado | `PATCH /api/alertas/{id}/estado` declarado | NO VERIFICADO; lanza `RuntimeException` genérica si no encuentra | `AlertaController.java:30-35`, `AlertaService.java:42-48` |
| Consulta geoespacial `$nearSphere` | Sí, con `NearQuery` sobre `MongoTemplate` | Lógica correcta a primera vista; **nunca se entrega el resultado** — comentario `// TODO: despachar notificaciones FCM` | `AlertaService.java:50-67` |
| Endpoints de Usuario / Dispositivo / Avistamiento | **No existen** — no hay controllers ni services | No funcionan: no se puede registrar usuario, registrar dispositivo, ni reportar avistamiento por API | Ausencia en `controller/` y `service/` (solo hay `AlertaController` y `AlertaService`) |
| Autenticación / JWT / bcrypt | **No declarado** — `pom.xml` no incluye `spring-boot-starter-security` ni librería JWT; no hay `SecurityConfig` | No funciona | `backend/pom.xml:28-55` (sin security/jwt) |
| Firebase FCM | **No declarado** — `pom.xml` no incluye `firebase-admin`; la integración es un comentario `// TODO` | No funciona, no notifica nada | `AlertaService.java:66`, `pom.xml:28-55` |
| DTOs / Bean Validation | No existen | — | Ausencia de `dto/`, `@Valid`, `@NotNull` |
| Manejo de excepciones / `@ControllerAdvice` | No existe | Errores caen como 500 con stack trace | Ausencia de `exception/` y `RuntimeException` literal en `AlertaService.java:44` |
| Tests | No existen | — | `backend/src/test/` no existe (solo `src/main/`) |
| Frontend (React/Tailwind/PWA/Leaflet) | **Directorio `frontend/` vacío** (0 archivos) | No existe | `ls frontend/` → vacío |
| Almacenamiento de fotos (`./uploads`) | No existe | No se guardan fotos; `foto_url` es un string sin servir nada | Ausencia de carpeta y endpoint |
| Init de Mongo (colecciones + índices + seed) | Sí, `mongo/init/01_init.js` completo y correcto | Se ejecuta vía Docker entrypoint al primer boot del contenedor | `mongo/init/01_init.js:9-71`, `docker-compose.yml:10` |
| `docker-compose.yml` | Sí | NO VERIFICADO; en teoría levanta Mongo + backend, pero **no monta replica set** pese a que README y `DATABASE_DESIGN.md` lo prometen | `docker-compose.yml:3-27` |
| Documentación de API (OpenAPI/Swagger) | No existe | — | `pom.xml` sin springdoc; sin `/swagger-ui` |
| Manual de usuario | No existe | — | — |

**Resumen del inventario:** lo único realmente operable a nivel API (asumiendo que compila) es **el módulo de Alertas, sin notificaciones y sin auth**. Todo el resto del scope (usuarios, dispositivos, avistamientos, FCM, frontend, auth) está en cero.

---

## 2. Compila / levanta — estado

| Pregunta | Respuesta | Cómo verificarlo cuando haya entorno |
|---|---|---|
| ¿El backend compila? | **NO VERIFICADO**. Lectura estática: el código no muestra imports rotos ni dependencias faltantes para lo que toca. Lombok + Spring Data Mongo están en `pom.xml`. | `cd backend && mvn clean compile` |
| ¿El backend levanta y se conecta a Mongo? | **NO VERIFICADO**. La URI por defecto apunta a `localhost:27017`. | `mvn spring-boot:run` con Mongo arriba |
| ¿El frontend buildea? | **No, porque no existe.** El directorio `frontend/` está vacío. | — |
| ¿Docker Compose levanta todo? | **NO VERIFICADO** (daemon no corriendo). En el papel, Mongo + backend deberían levantar. El `init/01_init.js` solo corre la primera vez (volumen vacío). | `docker compose up --build` |
| ¿Las llamadas E2E (crear alerta → notificar) funcionan? | **No.** Aunque la API arranque, FCM es un `// TODO` y no hay cliente que ejerza el flujo. | — |

No hay wrapper `mvnw` en `backend/`. Si en CI/grading el evaluador no tiene Maven instalado, no puede correrlo. **Sugerencia operativa:** generar el Maven Wrapper (`mvn -N wrapper:wrapper`) en algún momento antes de la entrega.

---

## 3. Rúbrica — Parcial 1 (Diseño, 25%) y Parcial 2 (Implementación, 30%)

Niveles: **I**nsuficiente / **A**ceptable / **B**ueno / **E**xcelente. Sin evidencia no se asigna nivel.

### Parcial 1 — Diseño (25%)

| # | Ítem | Peso | Nivel actual | Gap para llegar a Bueno/Excelente | Evidencia |
|---|---|---|---|---|---|
| 1 | Definición del problema | 5% | **Bueno (B)** | Para Excelente: alcance ya está definido, pero conviene aclarar más explícitamente en `README.md` el "no es Alerta Sofía, complementa" (hoy está disperso). | `SearchLink TPO.docx §1.2-1.5`, `README.md:23-62`, slides 2-4 |
| 2 | Modelado de datos NoSQL | 8% | **Bueno (B)** — el documento es sólido, pero hay **inconsistencia entre presentación y código** que baja el ítem | Para Excelente: (a) unificar el modelo del slide 8 con el del código (4 colecciones, no 3); (b) documentar embedding-vs-ref de forma explícita por colección; (c) justificar TTL de `expira_en` con un default y validación en el modelo (hoy el campo se persiste pero nada lo setea desde la app). | `DATABASE_DESIGN.md:1-307` (excelente), `mongo/init/01_init.js:9-53`, modelos en `backend/src/main/java/.../model/` — pero contradice slide 8 |
| 3 | Arquitectura propuesta | 6% | **Aceptable (A)** — bajan por: `README.md` contradice al código (Node.js vs Java); flujo de FCM está descrito pero no implementado; replica set descrito pero no configurado | Para Bueno: corregir `README.md` para que diga Spring Boot/Java; sacar las menciones a replica set 3 nodos o realmente configurarlo; agregar diagrama con la decisión `dispositivos` separada de `usuarios`. | `ARCHITECTURE.md:1-140` (bien), pero `README.md:95-100, 188-214` (mal) |
| 4 | Selección y justificación tecnológica | 6% | **Bueno (B)** para la base de datos; **Aceptable (A)** para el resto | Para Excelente: la comparación MongoDB vs PostgreSQL vs Cassandra está hecha (y en TPO se suma Neo4j/Redis). Falta justificar con comparación de alternativas: **Spring Boot vs Node/Express vs FastAPI**, **React vs Vue/Svelte para PWA**, **Leaflet vs Mapbox**, **FCM vs OneSignal/APNS+web-push**. Hoy se enuncian sin comparar. | `DATABASE_DESIGN.md:281-307`, `SearchLink TPO.docx §2.2.B` — pero solo cubre la base |

**Veredicto P1:** está al alcance cerrar P1 con nivel **Bueno** en los 4 ítems en pocas horas de trabajo de papel, sin tocar código. Hoy estaría en una mezcla A/B según corrección del evaluador.

### Parcial 2 — Implementación (30%)

| # | Ítem | Peso | Nivel actual | Gap | Evidencia |
|---|---|---|---|---|---|
| 5 | Implementación técnica | 9% | **Insuficiente (I)** | Sólo 1 de los 5 módulos del slide 5 / TPO §2.1 está parcialmente implementado (Alertas, sin notif). Faltan **todos** los demás: Auth/Roles, Avistamientos (endpoint), Geolocalización (más que el `nearSphere` ya hecho hay que exponerlo en API), Notificaciones (real). Frontend = 0. | `controller/AlertaController.java` único; ausencia de UsuarioController, DispositivoController, AvistamientoController, `SecurityConfig`, `FcmService`, `frontend/` vacío |
| 6 | Pipeline de datos (E2E) | 6% | **Insuficiente (I)** | No hay un solo flujo end-to-end funcionando: nadie puede registrarse, registrar un token FCM, recibir un push. Para Aceptable bastaría: registrar usuario → registrar dispositivo → publicar alerta → llegar push a ese dispositivo. | `AlertaService.java:66` (`// TODO FCM`), endpoints de usuario/dispositivo inexistentes |
| 7 | Calidad y performance | 5% | **Insuficiente (I)** — sin Aceptable porque no hay nada que medir | Para Aceptable: agregar paginación a `GET /api/alertas`, índices ya están creados (suma puntos cuando se demuestre con `explain()`), un test de integración mínimo con Testcontainers o Mongo embebido, manejo de excepciones global. | Ausencia de tests, `@ControllerAdvice`, paginación |
| 8 | Ajustes sobre diseño original | 5% | **Insuficiente (I)** — los ajustes existen pero no están documentados como tales | Para Bueno: redactar un changelog corto del diseño en `DATABASE_DESIGN.md` o `ARCHITECTURE.md` explicando: (a) por qué `dispositivos` se separó de `usuarios`, (b) por qué `alertas` reemplazó al nombre `perfiles_busqueda` del slide 8, (c) eliminación o no del replica set, (d) elección Spring vs Node si en algún momento se barajó Node (lo sugiere `README.md`). Sin documentar, los cambios parecen errores. | `README.md:95` (Node) vs código (Java) — divergencia no explicada |
| 9 | Documentación técnica | 5% | **Bueno (B)** — siempre que se corrija el `README.md` | Tres docs largas y bien escritas (`ARCHITECTURE.md`, `DATABASE_DESIGN.md`, `CLAUDE.md`), pero el `README.md` contradice al código. **Falta OpenAPI/Swagger** (`springdoc-openapi-starter-webmvc-ui` en `pom.xml` lo resuelve en 1 dependencia). | `ARCHITECTURE.md`, `DATABASE_DESIGN.md`, `CLAUDE.md`. Contraevidencia: `README.md:95-128` |

**Veredicto P2:** lejos. Para llegar a **Aceptable** en los 5 ítems hace falta cerrar al menos un pipeline E2E real, sumar los CRUDs faltantes y arreglar la inconsistencia documental. Como cuenta 30% del total, **es el frente de mayor ROI ahora**.

---

## 4. Entregables comprometidos (slide 11 + TPO §4)

| Entregable | Estado | Evidencia |
|---|---|---|
| Código fuente backend completo y documentado | **A medias** — backend existe pero cubre ~20% del scope funcional | `backend/src/main/java/...` |
| Código fuente frontend completo y documentado | **Ausente** | `frontend/` vacío |
| Documentación de arquitectura y diseño | **Cubierto** (parcialmente, con README desactualizado) | `ARCHITECTURE.md`, `DATABASE_DESIGN.md` |
| Documentación de la API REST (OpenAPI / Swagger) | **Ausente** | sin `springdoc` en `pom.xml`, sin `/swagger-ui` |
| Manual de usuario (admin + estándar) | **Ausente** | — |
| Instructivo de instalación y ejecución local (README) | **A medias** — existe pero describe stack equivocado | `README.md:186-214` |
| **Criterios de aceptación** | | |
| Módulos funcionales (sección 2.1) probados en local | **No cumple** — sólo Alertas parcialmente | — |
| Push verificable al publicar alerta | **No cumple** — FCM es TODO | `AlertaService.java:66` |
| Búsqueda geoespacial con 2dsphere | **Parcial** — la query existe en `notificarUsuariosCercanos`, no expuesta como endpoint testeable | `AlertaService.java:50-67` |
| Sin errores críticos abiertos | **NO VERIFICADO** | — |

---

## 5. Veredicto

**Recomendación: SEGUIR con lo existente, no reiniciar nada.** Reiniciar sería tirar a la basura tres documentos de diseño que son la mitad del Parcial 1 y un esqueleto Spring Boot correcto que ya cubre el modelado más complejo (geoespacial + TTL).

Razonamiento por costo/beneficio:

- **Costo de seguir:** el backend tiene una arquitectura limpia y consistente con lo que enseña la materia. Los modelos están bien mapeados a Mongo con `@GeoSpatialIndexed`. Lo que falta son los controllers/services que faltan (mecánico) y el frontend (sí, todo) y FCM (medio día con Firebase Admin SDK). No hay deuda técnica en lo que existe.
- **Costo de reiniciar:** se pierde `DATABASE_DESIGN.md` y `ARCHITECTURE.md`, que son la materia prima del P1; y no resuelve el problema real, que es **falta de scope implementado**, no mala calidad del implementado.

**Lo único que conviene "reiniciar puntualmente":**

1. **`README.md`** — reescribir de cero, alineado al código (Java/Spring/Mongo single-node, no Node/Replica Set). Es 1 hora de trabajo y desbloquea ítems 3, 8 y 9 de la rúbrica.
2. **Decidir y unificar el modelo nominal**: o renombrar `alertas` → `perfiles_busqueda` para coincidir con el slide 8, o documentar el cambio. Recomendado: mantener `alertas` (es más simple y honesto) y reflejarlo en una próxima presentación / actualizar slide.

---

## 6. Próximos pasos priorizados

Ordenados por ROI sobre nota (peso de la rúbrica / esfuerzo). Cada paso intenta cerrar uno o varios ítems.

### Bloque A — Cerrar P1 antes de Parcial 1 (esfuerzo: ~1 día de papel, 0 código)

1. **Reescribir `README.md`** para que describa Java/Spring/Mongo single-node (o configurar realmente el replica set). Cierra parte de ítem 3, ítem 8 y ítem 9.
2. **Agregar sección "Cambios sobre el diseño original" en `DATABASE_DESIGN.md`** explicando: separación de `dispositivos`, nombre `alertas` (vs `perfiles_busqueda` del slide 8), eliminación del replica set en MVP, FCM como `// TODO` con plan de integración. Cierra ítem 8.
3. **Sumar comparación de alternativas para Backend y Frontend** (Spring vs Node/FastAPI; React vs Vue/Svelte; Leaflet vs Mapbox). Una sección corta en `ARCHITECTURE.md`. Cierra ítem 4 nivel Excelente.
4. **Actualizar el slide 8 de la presentación** (o re-presentar) con las cuatro colecciones reales. Cierra el desfase narrativo.

### Bloque B — Cerrar P2 en nivel Aceptable (esfuerzo: ~3-5 días de código)

5. **Endpoints faltantes mínimos:**
   - `POST /api/usuarios` (registro con `passwordHash` y `ubicacion` GeoJSON)
   - `POST /api/dispositivos` (registrar token FCM contra `usuario_id`)
   - `POST /api/avistamientos` (reportar avistamiento atado a `alerta_id`)
   - `GET /api/alertas/{id}/avistamientos`
   Cierra ítem 5 en Aceptable y desbloquea ítem 6.
6. **Implementar el `// TODO FCM`** con `firebase-admin` SDK. Al crear una alerta, traer tokens de `dispositivos` de los usuarios cercanos y disparar el envío. Cierra ítem 6 en Aceptable/Bueno.
7. **`@ControllerAdvice` + DTOs + `@Valid`** — sacar las `RuntimeException` y validar entrada. Suma a ítem 5 e ítem 7.
8. **Springdoc-openapi** — una dependencia en `pom.xml` y se expone `/swagger-ui` con todos los endpoints. Cubre el entregable de "Documentación API REST".
9. **Maven Wrapper** (`mvn -N wrapper:wrapper`) para que el evaluador pueda correr `./mvnw spring-boot:run` sin instalar Maven.

### Bloque C — Cerrar el frontend (esfuerzo: bloque grande, fuera de P2 inmediato)

10. **Scaffolding React+Vite+Tailwind** dentro de `frontend/` con dos páginas mínimas: listado de alertas (con mapa Leaflet) y formulario de registro de usuario con captura de ubicación. Sólo esto ya destraba múltiples ítems del Final.

### Bloque D — Si hay aire (sube niveles, no rescata)

11. **Auth con Spring Security + JWT** (módulo del slide 5 y TPO §2.1). Es trabajo medianamente grande; sólo encarar si el resto del scope está cubierto.
12. **Tests de integración con Testcontainers + Mongo**. Sube ítem 7 a Bueno.
13. **Subir fotos** a `./uploads` con `MultipartFile`.

---

## 7. Preguntas abiertas (no las invento, las dejo a tu criterio)

Estos puntos del alcance no me cierran del todo y prefiero levantarlos antes de seguir tocando algo:

1. **¿El replica set 3-nodos es parte del scope que se va a defender, o se asume single-node?** Hoy el código tiene single-node pero el README y `DATABASE_DESIGN.md` lo presentan como replica set. Hay que elegir uno y mantenerlo.
2. **¿La presentación 1 se rehace para reflejar `alertas` / `dispositivos`, o se mantiene como está y se documenta el cambio?** Esto afecta directamente el ítem 8.
3. **¿La autenticación JWT es objetivo de P2 o se difiere al Final?** El slide 5 y TPO §2.1 la enumeran como módulo. Si va a P2 es trabajo grande y conviene priorizarlo ya.
4. **¿La PWA / Leaflet es objetivo de P2 o de Final?** Hoy no hay nada de frontend. Si se compromete para P2, el orden de prioridades del Bloque B/C cambia drásticamente.

---

# Decisiones — 2026-05-22 (segunda corrida)

Decisiones cerradas para entrar al P2 con un pipeline E2E delgado. No se re-litigan decisiones de scope ya tomadas (AUTH mínima, frontend mínimo, modelado no se documenta como ajuste todavía).

## D1. Modelado: 3 colecciones (embebido) vs 4 (separado) — veredicto

**Veredicto: VOLVER A 3 COLECCIONES, embebiendo `fcm_tokens` (array) en `usuarios`. El slide 8 NO se rehace.**

### Patrones de acceso reales (no inferidos)

El path crítico del MVP es **despachar push al publicar una alerta**:

| Patrón con 4 colecciones (estado actual) | Patrón con 3 colecciones (embebido) |
|---|---|
| 1. `$nearSphere` sobre `usuarios` → IDs de usuarios cercanos. 2. `findByUsuarioIdInAndActivoTrue(...)` sobre `dispositivos` → tokens. **2 queries.** | 1. `$nearSphere` sobre `usuarios` proyectando `fcm_tokens` → tokens directo. **1 query.** |

`AlertaService.notificarUsuariosCercanos` (`backend/src/main/java/ar/edu/uade/searchlink/service/AlertaService.java:50-67`) hoy sólo hace la primera mitad y deja el dispatch como TODO — es decir, el segundo lookup aún no está pagado, pero estaría obligado a hacerlo si se mantiene la separación.

### Análisis de los argumentos de la separación, sin tibieza

| A favor de separación (4 colecciones) | Vale acá? |
|---|---|
| Update granular de tokens sin reescribir el doc del usuario | No materialmente. Mongo permite `$push`/`$pull`/`$set` atómicos sobre el array embebido; el "reescribir doc completo" es marketing, no realidad. |
| `ultimo_uso` por token sin tocar el doc del usuario | Lo mismo: campo dentro de cada elemento del array, `$set` atómico. |
| Array acotado vs unbounded | Un usuario tiene 1–3 dispositivos en este dominio (móvil + tablet + web). No hay riesgo de aproximarse a 16 MB. |
| Tokens FCM rotan a menudo | Sí, pero el flujo es: el cliente reporta nuevo token → backend hace `$pull` del viejo + `$push` del nuevo, o `$addToSet`. Es 1 operación contra el doc del usuario; la separación no la simplifica. |

### Decisión y consecuencias

- Es **mejora retroceder** a 3 colecciones: alinea el código con el slide 8 (que ya quedó en la presentación entregada), con la justificación del TPO (`SearchLink TPO.docx §2.2.A`: "lectura rápida con documentos embebidos"), y con el patrón de acceso real (1 query en el path crítico).
- **Slide 8 NO se rehace.**
- **No se documenta como "ajuste sobre el diseño original"**; al revés, se elimina el ajuste no documentado que tenía el código.
- Se elimina del backend: `model/Dispositivo.java`, `repository/DispositivoRepository.java`, enum `Plataforma.java` (o se reubica como campo del item embebido).
- `Usuario.java` gana un campo `List<DispositivoEmbebido>` o `List<FcmToken>` con sub-campos `{ fcm_token, plataforma, activo, ultimo_uso, registrado_en }`.
- `mongo/init/01_init.js` deja de crear la colección `dispositivos` y sus índices.
- Esto se hará en el paso 1 del plan P2.

## D2. Replica set — rama aplicada

**Rama aplicada: NO montar replica set 3-nodos. Cubrir Unidad IV en papel.**

**Qué decía la diapositiva (última de la única presentación entregada, `SearchLink_Presentacion 1.pptx`, slide 11 "Entregables y criterios de aceptación"), literal:**

> *Entregables:* Código fuente completo y documentado (backend + frontend). Documentación de arquitectura y diseño. Documentación de la API REST (OpenAPI / Swagger). Manual de usuario (administrador y estándar). Instructivo de instalación y ejecución local (README).
> *Criterios de aceptación:* Módulos funcionales (todos los de §2.1 probados en local). Push verificable (al publicar una alerta, dispositivos en el radio reciben push). Búsqueda geoespacial (resultados correctos con índices 2dsphere). Sin errores críticos.

No aparecen las palabras *replica set*, *replicación*, *3 nodos*, *cluster*, *sharding* ni *consistencia* en esa diapositiva. Verificado con grep sobre el XML del slide. (Aclaración: no existe una "presentación final" separada en `Projects/Presentaciones/`; sólo está `SearchLink_Presentacion 1.pptx`. Trato su slide 11 como contrato vigente porque es el único compromiso documentado.)

Consecuencias:

- No se monta replica set en Docker Compose. El compose sigue con Mongo único.
- Se elimina del `README.md` y del `DATABASE_DESIGN.md` toda mención a "Replica Set de 3 nodos" como parte del MVP — quedaba como ruido.
- Unidad IV (CAP / consistencia / replicación) se cubre en papel dentro de `DATABASE_DESIGN.md` o un anexo: estrategia de replicación que se elegiría, posicionamiento de MongoDB en CAP (MongoDB = CP en operaciones de escritura sobre primario; con `readPreference=secondary` se gana A perdiendo C), y por qué para SearchLink se elegiría consistencia fuerte sobre el primario en escrituras críticas (alertas) y opcional lectura desde secundarios para listados de baja sensibilidad. Esto cubre la nota teórica sin tocar infra.
- Tratado como BLOQUE AISLADO al final del plan: no es dependencia del E2E.

## D3. Plan de trabajo P2 — priorizado por ROI

Ordenado para que el paso 1 sea el camino mínimo al **pipeline E2E funcionando** (criterio explícito de la consigna). Cada paso indica ítem de rúbrica atacado, entrega concreta, y dependencia.

### Paso 0 — README + ESTADO actualizados [HECHO en esta corrida]

- Ítem(s): 3, 8, 9.
- Entrega: `README.md` reescrito para reflejar Spring Boot + Mongo único; `ESTADO.md` ampliado con esta sección de decisiones.
- Dependencias: ninguna.

### Paso 1 — Revertir a 3 colecciones (embebido) — ✅ COMPLETO (commit `b109f42`)

- Ítem(s): 2 (modelado), 9 (consistencia documental). Defensiva para 8 (al eliminar la divergencia, deja de existir un "ajuste no documentado" a explicar).
- Entrega: `Usuario.java` con array embebido de tokens, eliminación de `Dispositivo.java`/`DispositivoRepository.java`, ajuste de `AlertaService.notificarUsuariosCercanos`, actualización de `mongo/init/01_init.js`.
- Dependencia: ninguna; **debe ir antes de tocar más endpoints**, porque cambia la firma del despacho push.
- **Hecho (commit `b109f42`):**
  - 3 colecciones; `dispositivos` embebido como sub-objetos en `Usuario` (`{ fcm_token, plataforma, activo, ultimo_uso, registrado_en }`).
  - Borrados `Dispositivo.java`, `DispositivoRepository.java`, enum `Plataforma.java`.
  - Modelo de ubicación opción B: `ubicacion_precargada` (2dsphere normal, obligatoria) + `ubicacion_actual` (2dsphere sparse, GPS con consentimiento). El push de esta fase corre sobre `ubicacion_precargada`.
  - `mongo/init/01_init.js`: 3 colecciones, índices 2dsphere + sparse + TTL, seeds para QA de ambas ramas del coalesce.
  - `docs/DATABASE_DESIGN.md` creado (reemplaza el `DATABASE_DESIGN.md` raíz, eliminado); documenta modelo, embedding, ubicación B, divergencias vs Entrega 1 slide 8 (nombre `perfiles_busqueda`→`alertas` y tokens planos→sub-objetos), y decisiones pendientes (§5.1 unique fcm_token, §5.2 distancia avistamientos, §5.3/§3.5 combinación de queries geo).
  - Fix `annotationProcessorPaths` (Lombok) en `pom.xml` para que compile vía Maven CLI.
- **Deuda abierta del Paso 1** (documentada, no bloqueante): rehacer el slide 8 de la **presentación final** para que diga `alertas` (no `perfiles_busqueda`).

### Próximo — Auth (JWT + bcrypt + roles)

- Lo que sigue tras el Paso 1 es la capa de autenticación: hashing de contraseñas con **bcrypt**, emisión/validación de **JWT** firmado, y autorización por **roles** (ciudadano vs operador). Hoy ausente por completo (sin Spring Security, sin JWT, sin bcrypt en `pom.xml` ni en código).
- Nota: el Paso 2 original abajo planteaba auth *liviana* (token opaco) como atajo del E2E; la decisión vigente es ir directo a JWT+bcrypt+roles. Revisar el alcance del Paso 2 contra esto antes de implementarlo.

### Paso 2 — Endpoints faltantes mínimos del backend (camino mínimo del E2E)

- Ítem(s): 5 (implementación) → de Insuficiente a Aceptable; 6 (pipeline E2E) → habilita el flujo.
- Entrega concreta (sin auth fuerte; rol resuelto por header o por query param de usuario, lo justo para distinguir admin vs estándar):
  - `POST /api/usuarios` — registro con `email`, `password_hash` (bcrypt, dependencia liviana, no Spring Security), `rol`, `ubicacion` GeoJSON.
  - `POST /api/sesiones` — login mínimo: valida hash, devuelve un identificador de sesión simple (no JWT firmado; un token opaco en memoria o el `usuario_id` mismo). Se reemplaza por JWT en el Final.
  - `POST /api/usuarios/{id}/dispositivos` — registrar/actualizar token FCM (`$addToSet` sobre el array embebido).
  - `POST /api/avistamientos` — reporta avistamiento atado a `alerta_id`.
  - `GET /api/alertas/{id}/avistamientos` — lista para el operador.
  - `GET /api/alertas` ya existe; agregar paginación básica.
  - `@ControllerAdvice` con respuestas 400/404/409 estándar.
- Dependencia: Paso 1.

### Paso 3 — Push real con Firebase Admin SDK

- Ítem(s): 6 (pipeline E2E) → de Insuficiente a Bueno; criterio de aceptación slide 11 ("Push verificable").
- Entrega: `firebase-admin` en `pom.xml`, `FcmService` con `MulticastMessage`, credenciales por env var, reemplazo del `// TODO` en `AlertaService:66`. Test manual: token de prueba registrado recibe el push al publicar alerta.
- Dependencia: Pasos 1 y 2.

### Paso 4 — Frontend mínimo

- Ítem(s): 5, 6 (cierra el E2E visible).
- Entrega: React + Vite (sin Tailwind, sin PWA, sin Leaflet) con 3-4 pantallas: login, lista de alertas, formulario nuevo avistamiento (input lat/lng manual, sin mapa todavía), formulario nueva alerta (admin). Persistencia de sesión en `localStorage` con el token opaco del Paso 2.
- Dependencia: Pasos 1, 2, 3.

### Paso 5 — Documentación API (OpenAPI / Swagger)

- Ítem(s): 9 (entregable comprometido en slide 11); aporta a 3.
- Entrega: dependencia `springdoc-openapi-starter-webmvc-ui` y anotación mínima en los controllers; `/swagger-ui` accesible.
- Dependencia: Paso 2 (porque documenta los endpoints reales).

### Paso 6 — Unidad IV en papel

- Ítem(s): cubre la teoría (relevante para la materia, no aparece como ítem específico de la rúbrica P2 pero se cruza con ítem 2 y con la defensa individual del Final).
- Entrega: sección "Replicación, CAP y consistencia" en `DATABASE_DESIGN.md`, ~1 página: justifica MongoDB = CP, estrategia de replicación que se montaría en producción, por qué no se monta en MVP, qué se elegiría en `readPreference`/`writeConcern`.
- Dependencia: ninguna; bloque aislado.

### Paso 7 — Maven Wrapper

- Ítem(s): 9 (instructivo de instalación reproducible).
- Entrega: `mvn -N wrapper:wrapper` desde un entorno con Maven disponible (no éste); commit del `mvnw`, `mvnw.cmd`, `.mvn/`.
- Dependencia: ninguna.

### Paso 8 — Tests de integración mínimos [opcional, si hay aire]

- Ítem(s): 7 (calidad/performance) → de Insuficiente a Aceptable/Bueno.
- Entrega: Testcontainers + Mongo, un test que ejerce el flujo crear-usuario → crear-alerta → query geoespacial. `explain()` opcional para demostrar uso de índice.
- Dependencia: Pasos 1 y 2.

### Lo que NO entra en P2 (queda para Final)

- JWT firmado, recuperación de contraseña, registro con verificación de email.
- PWA, service worker, instalación móvil.
- Leaflet + OpenStreetMap.
- Subida de fotos reales (campo `foto_url` queda como string sin servir nada).
- Replica set montado de verdad.
- Verificación / moderación de avistamientos por admin como flujo completo (queda como flag `verificado` en el modelo, sin UI).

