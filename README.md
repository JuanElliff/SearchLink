# SearchLink

Plataforma web de alertas de personas desaparecidas. Cuando un operador publica una alerta, el
sistema localiza por consulta geoespacial (`$nearSphere` / índice `2dsphere`) a los usuarios
registrados dentro de un radio configurable y les dispara una notificación push (estilo Alerta
Sofía). Los ciudadanos pueden reportar avistamientos georreferenciados.

**TP Integrador — Ingeniería de Datos II · UADE.** Pensado para correr en un entorno de desarrollo
local.

---

## Stack

| Capa | Tecnología |
|---|---|
| Backend | **Spring Boot 3.3.4 · Java 21** (Spring Web, Spring Data MongoDB, Spring Security, Bean Validation, Lombok) |
| Base de datos | **MongoDB 7** (instancia única; índices `2dsphere` + TTL) |
| Notificaciones | **Firebase Cloud Messaging** (push) |
| Frontend | **React 19 · Vite · Tailwind · react-leaflet · PWA** |
| Documentación API | **springdoc-openapi / Swagger UI** |

---

## Requisitos previos

- **Docker + Docker Compose** — para el arranque recomendado.
- **Node + npm** — para el frontend. El repo **no fija** una versión de Node (sin `engines` en
  `package.json`, sin `.nvmrc`); se usó una versión de Node compatible con Vite.
- **JDK 21** — **solo** si se corre el backend **sin Docker** (ver más abajo).

---

## Arranque rápido (con Docker)

Levanta MongoDB y el backend:

```bash
docker compose up --build
```

- **MongoDB** en `localhost:27017`. El seed `mongo/init/01_init.js` corre **automáticamente al
  primer boot** (cuando el volumen está vacío): crea las colecciones, los índices y **4 usuarios
  QA** (ver tabla más abajo).
- **Backend** Spring Boot en `localhost:8080`, conectado a Mongo por la red interna.

El **frontend NO está en el compose**: se levanta aparte.

```bash
cd frontend
npm install
npm run dev          # Vite en http://localhost:5173
```

> **FCM en Docker queda DESHABILITADO por defecto:** el compose no pasa `FIREBASE_CREDENTIALS_PATH`,
> así que el backend arranca igual pero sin enviar push. Ver la sección Firebase / FCM para
> habilitarlo.

---

## Arranque sin Docker (backend local)

Requiere **JDK 21**, Maven y un MongoDB 7 corriendo en `localhost:27017`.

> ### ⚠️ NOTA CRÍTICA — JDK 21 obligatorio
>
> El Maven del host suele correr sobre **JDK 26** (default de Homebrew), que **rompe Lombok** al
> compilar:
> ```
> java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN
> ```
> Antes de `mvn test` o `mvn spring-boot:run` hay que apuntar `JAVA_HOME` a **JDK 21**:
>
> ```bash
> export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
> # alternativa portátil:
> export JAVA_HOME=$(/usr/libexec/java_home -v 21)
> ```
>
> Esto aplica **solo sin Docker**. Dentro de `docker compose` el build usa
> `maven:3.9-eclipse-temurin-21`, así que no hay que setear nada.

```bash
cd backend
mvn spring-boot:run      # levanta en :8080
```

Si Mongo se levanta a mano (sin Docker), cargar los seeds la primera vez:

```bash
mongosh < mongo/init/01_init.js
```

---

## Variables de entorno

### Backend

Todas tienen default (la app arranca sin definir ninguna).

| Variable | Default | Para qué |
|---|---|---|
| `MONGO_URI` | `mongodb://localhost:27017/searchlink` | Cadena de conexión a Mongo (en Docker: `mongodb://mongodb:27017/searchlink`). |
| `PORT` | `8080` | Puerto del backend. |
| `SEARCHLINK_JWT_SECRET` | secret de dev | Clave de firma HS256 del JWT (cambiar en producción). |
| `SEARCHLINK_JWT_EXPIRATION_MS` | `86400000` (24 h) | Vigencia del access token. |
| `FIREBASE_CREDENTIALS_PATH` | *(vacío)* | Ruta al `serviceAccountKey.json`. Vacío → **FCM deshabilitado**. |
| `SEARCHLINK_UPLOADS_DIR` | `uploads` | Carpeta en disco para las fotos subidas. |

### Frontend

Copiar `frontend/.env.example` a `frontend/.env` y completar las **7** variables:

```
VITE_API_BASE_URL
VITE_FIREBASE_API_KEY
VITE_FIREBASE_AUTH_DOMAIN
VITE_FIREBASE_PROJECT_ID
VITE_FIREBASE_MESSAGING_SENDER_ID
VITE_FIREBASE_APP_ID
VITE_FIREBASE_VAPID_KEY
```

---

## Firebase / FCM

Para habilitar el push real hacen falta dos cosas:

1. **Backend:** un `serviceAccountKey.json` de la cuenta de servicio del proyecto Firebase, apuntado
   por `FIREBASE_CREDENTIALS_PATH`. El archivo está **gitignored** (nunca se commitea). Sin él, el
   backend arranca igual pero no envía push.
2. **Frontend:** las `VITE_FIREBASE_*` cargadas en `frontend/.env`.

> **Config del cliente en DOS lugares.** Si se forkea a otro proyecto Firebase, hay que actualizar
> **ambos**:
> - `frontend/.env` → las `VITE_FIREBASE_*` (que consume la app).
> - `frontend/public/firebase-messaging-sw.js` → el `firebaseConfig` está **hardcodeado** en el
>   service worker (un SW no puede leer `import.meta.env`).

---

## Usuarios de prueba (QA)

Cargados por el seed `mongo/init/01_init.js`:

| Email | Password | Rol |
|---|---|---|
| `admin@searchlink.dev` | `Admin1234` | ADMIN |
| `operador@searchlink.dev` | `Operador1234` | OPERADOR |
| `belgrano@searchlink.dev` | `Estandar1234` | ESTANDAR |
| `caballito@searchlink.dev` | `Estandar1234` | ESTANDAR |

---

## Documentación de la API

Con el backend levantado:

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **Spec OpenAPI:** http://localhost:8080/v3/api-docs

Para probar endpoints protegidos: obtener un JWT con `POST /api/sesiones` (login), tocar el botón
**Authorize** de Swagger UI y pegar el token. A partir de ahí Swagger manda `Authorization: Bearer
<token>` en cada request.

---

## Tests

```bash
mvn test          # con JAVA_HOME=JDK 21 si es sin Docker (ver nota crítica)
```

**67 tests verdes.** Usan **Flapdoodle** (MongoDB embebido), así que **no requieren** un Mongo
corriendo.

---

## Equipo

Pasman, Micaela · Di Napoli, Máximo · Elliff, Juan Cruz · Amorin, Facundo · Naconyszny, Rodrigo.
