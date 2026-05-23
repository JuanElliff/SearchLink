# SearchLink

Plataforma web de alertas geolocalizadas para la búsqueda de personas desaparecidas.

**TP Integrador — Ingeniería de Datos II · UADE**
Materia: Bases de Datos NoSQL — Arquitectura y Modelado.

---

## Tabla de contenidos

1. [Problema](#problema)
2. [Solución](#solución)
3. [Stack](#stack)
4. [Modelo de datos](#modelo-de-datos)
5. [Estructura del repositorio](#estructura-del-repositorio)
6. [Cómo levantar el proyecto](#cómo-levantar-el-proyecto)
7. [Estado de implementación](#estado-de-implementación)
8. [Documentación adicional](#documentación-adicional)

---

## Problema

En Argentina, la difusión de alertas de personas desaparecidas depende de canales fragmentados (redes sociales, medios tradicionales, mensajería informal). Las primeras horas son críticas y no existe un canal estructurado que (a) centralice la información, (b) notifique de forma inmediata a usuarios cercanos al punto de desaparición, y (c) reciba avistamientos ciudadanos georreferenciados.

---

## Solución

SearchLink centraliza la emisión de alertas y los reportes de avistamiento. Cuando un operador publica una alerta, el sistema localiza por consulta geoespacial (`$nearSphere`, índice `2dsphere`) a los usuarios registrados dentro de un radio configurable y dispara notificaciones push vía Firebase Cloud Messaging.

```
Operador carga alerta
        │
        ▼  POST /api/alertas
Backend persiste en MongoDB (GeoJSON Point + TTL)
        │
        ▼  $nearSphere sobre usuarios (radio configurable)
Backend obtiene tokens FCM de los usuarios cercanos
        │
        ▼  Firebase Cloud Messaging
Dispositivos del área reciben push
        │
        ▼
Ciudadano puede reportar avistamiento (POST /api/avistamientos)
```

---

## Stack

| Capa | Tecnología | Notas |
|---|---|---|
| Backend | **Java 21 + Spring Boot 3.3.4** | API REST. `spring-boot-starter-web`, `spring-boot-starter-data-mongodb`, Lombok. |
| Base de datos | **MongoDB 7** | Instancia única (no replica set en MVP). Índices `2dsphere` para consultas geoespaciales y TTL sobre alertas vencidas. |
| Notificaciones | **Firebase Cloud Messaging** | Único servicio externo (en la nube). En implementación; ver §Estado. |
| Frontend | **React** (a construir) | Mínimo para demostrar el flujo E2E en P2; PWA / Tailwind / Leaflet quedan para el Final. |
| Infraestructura local | **Docker Compose** | Convenience para desarrollo, no parte de la arquitectura. |

Java 21 está fijado en `backend/pom.xml`. El proyecto no usa `mvnw` todavía: requiere Maven instalado para correr fuera de Docker (ver §Cómo levantar).

---

## Modelo de datos

Tres colecciones en la base `searchlink`:

| Colección | Propósito | Índices |
|---|---|---|
| `usuarios` | Cuentas (ciudadano / operador / admin), ubicación declarada y tokens FCM de los dispositivos del usuario (embebidos como array). | `2dsphere` sobre `ubicacion`; único sobre `email`. |
| `alertas` | Alerta publicada: datos del desaparecido, geolocalización, estado, expiración. | `2dsphere` sobre `ubicacion`; sobre `estado`; TTL sobre `expira_en`. |
| `avistamientos` | Reporte ciudadano asociado a una alerta. | `2dsphere` sobre `ubicacion`; sobre `alerta_id`; sobre `creado_en` desc. |

Justificación detallada de modelado, índices, embedding vs referencia, y comparación con otras bases NoSQL en [`DATABASE_DESIGN.md`](DATABASE_DESIGN.md).

> Nota sobre el código actual: hoy el backend tiene una cuarta colección `dispositivos` separada. Se va a fusionar en `usuarios` (tokens FCM embebidos) en el primer paso del plan de implementación de P2 — ver §Estado y [`ESTADO.md`](ESTADO.md) §D1.

---

## Estructura del repositorio

```
SearchLink/
├── backend/                       # API Spring Boot
│   ├── src/main/java/ar/edu/uade/searchlink/
│   │   ├── SearchLinkApplication.java
│   │   ├── config/                # MongoConfig (MongoTemplate bean)
│   │   ├── model/                 # @Document classes + enums
│   │   ├── repository/            # MongoRepository interfaces
│   │   ├── service/               # AlertaService (geoespacial + dispatch)
│   │   └── controller/            # AlertaController
│   ├── src/main/resources/application.properties
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/                      # Pendiente — React/Vite mínimo (P2)
│
├── mongo/
│   └── init/01_init.js            # Init: colecciones, índices y seed (auto al primer boot)
│
├── docker-compose.yml             # Mongo + backend en red interna
├── ARCHITECTURE.md                # Arquitectura en capas, flujos y decisiones
├── DATABASE_DESIGN.md             # Modelado, índices, justificación NoSQL
├── ESTADO.md                      # Diagnóstico vigente + decisiones de implementación
└── README.md                      # Este archivo
```

---

## Cómo levantar el proyecto

### Opción A — Docker Compose (recomendado)

Requiere Docker y Docker Compose.

```bash
docker compose up --build
```

Esto levanta:

- `mongodb` en `localhost:27017` (instancia única; `mongo/init/01_init.js` se ejecuta al primer boot y crea colecciones + índices + un usuario operador de prueba).
- `backend` Spring Boot en `localhost:8080`, conectado a Mongo por nombre de red interna (`mongodb`).

Variables de entorno por defecto en `docker-compose.yml`:

| Variable | Default | Para qué |
|---|---|---|
| `MONGO_URI` | `mongodb://mongodb:27017/searchlink` | Cadena de conexión a Mongo. |
| `PORT` | `8080` | Puerto del backend. |

### Opción B — Sin Docker

Requiere Java 21 y Maven instalados, y MongoDB 7 corriendo en `localhost:27017`.

```bash
# Backend
cd backend
mvn clean compile        # compila
mvn spring-boot:run      # levanta en :8080
mvn test                 # tests (cuando existan)
mvn package -DskipTests  # genera el JAR en target/
```

La conexión a Mongo se toma de `MONGO_URI` si está seteada; si no, `mongodb://localhost:27017/searchlink` (ver `backend/src/main/resources/application.properties`).

Si se levanta Mongo a mano (sin Docker), correr `mongo/init/01_init.js` con `mongosh < mongo/init/01_init.js` la primera vez para crear los índices y el usuario seed.

### Endpoints implementados hoy

Ver `backend/src/main/java/ar/edu/uade/searchlink/controller/`. Endpoints actualmente expuestos:

| Método | Ruta | Estado |
|---|---|---|
| `POST` | `/api/alertas` | Crea alerta. Ejecuta consulta geoespacial sobre `usuarios`. El despacho FCM real está pendiente. |
| `GET` | `/api/alertas` | Lista alertas en estado `ACTIVA`. |
| `PATCH` | `/api/alertas/{id}/estado?estado={ACTIVA|RESUELTA|CANCELADA}` | Cambia el estado. |

Los endpoints de usuarios, dispositivos (tokens FCM) y avistamientos están en el plan de implementación del P2 — ver [`ESTADO.md`](ESTADO.md).

---

## Estado de implementación

| Bloque | Estado |
|---|---|
| Esqueleto Spring Boot + conexión Mongo | Hecho |
| Modelos + índices (incluidos `2dsphere` y TTL) | Hecho |
| Endpoints de Alerta (crear / listar / cambiar estado) | Hecho |
| Consulta geoespacial `$nearSphere` sobre `usuarios` | Hecho (lógica); falta el dispatch al final |
| Push real a Firebase FCM | Pendiente (TODO en `AlertaService`) |
| Endpoints de Usuario / Avistamiento / registro de token FCM | Pendiente |
| Auth mínima (rol admin vs estándar) | Pendiente (JWT robusto queda para Final) |
| Frontend | Pendiente |
| Documentación OpenAPI / Swagger | Pendiente |
| Replica set / replicación | No forma parte del MVP — cubierto en papel en `DATABASE_DESIGN.md` |

Plan de trabajo priorizado por ROI sobre la rúbrica en [`ESTADO.md`](ESTADO.md) §D3.

---

## Documentación adicional

- [`ARCHITECTURE.md`](ARCHITECTURE.md) — Arquitectura en capas, servicios, flujo principal de una alerta.
- [`DATABASE_DESIGN.md`](DATABASE_DESIGN.md) — Modelado de colecciones, decisiones de embedding/referencia, índices, comparación con otras bases NoSQL.
- [`ESTADO.md`](ESTADO.md) — Inventario real, rúbrica con evidencia, decisiones de implementación, plan P2 priorizado.

---

## Equipo

Pasman, Micaela · Di Napoli, Máximo · Elliff, Juan Cruz · Amorin, Facundo · Naconyszny, Rodrigo.
