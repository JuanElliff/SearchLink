# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SearchLink is a geolocation-based missing child alert platform (TP Integrador for Ingeniería de Datos II at UADE). When an alert is posted, the system executes a `$nearSphere` MongoDB query to find registered users within ~10 km and dispatches Firebase FCM push notifications to their devices.

## Commands

### Run with Docker (recommended)
```bash
docker compose up --build
```

### MongoDB Replica Set initialization (first time only)
```bash
docker exec -it searchlink-mongo mongosh --eval "rs.initiate({_id:'rs0',members:[{_id:0,host:'mongo:27017'}]})"
```

### Backend (without Docker)
```bash
cd backend
mvn spring-boot:run          # Run
mvn clean compile            # Compile
mvn test                     # Run tests
mvn package -DskipTests      # Build JAR
```

## Architecture

**Stack:** Java 21, Spring Boot 3.3.4, Spring Data MongoDB, Lombok, MongoDB 7, Docker Compose.

**Three layers:** `frontend/` (empty stub) → Spring Boot REST API (`:8080`) → MongoDB (`:27017`).

**Backend package root:** `ar.edu.uade.searchlink`

```
backend/src/main/java/ar/edu/uade/searchlink/
├── config/MongoConfig.java          # MongoTemplate bean for custom geospatial queries
├── model/                           # MongoDB @Document classes + enums
├── repository/                      # Spring Data MongoRepository interfaces
├── service/AlertaService.java       # Business logic; NearQuery via MongoTemplate
└── controller/AlertaController.java # REST endpoints
```

**MongoDB collections and their purpose:**
- `usuarios` — registered citizens/operators; stores GeoJSON Point `ubicacion`; `2dsphere` index enables `$nearSphere` queries
- `dispositivos` — FCM device tokens linked to users
- `alertas` — missing child alert documents; GeoJSON Point for location; TTL index on `expira_en` auto-deletes expired alerts
- `avistamientos` — citizen sighting reports (kept separate to avoid document-size growth on alertas)

**Key design pattern:** When an alert is published, `AlertaService` queries *usuarios* by proximity to the alert location (not the other way around), retrieves their FCM tokens from *dispositivos*, and calls Firebase FCM. FCM dispatch is currently a TODO stub.

**Geospatial queries** use `MongoTemplate` + `NearQuery` / `$nearSphere` — not Spring Data derived query methods — because the standard repository interfaces don't support geo queries with dynamic distance parameters.

**Initialization:** `mongo/init/01_init.js` runs on first container start; creates all collections, indexes, and a seed operator user (`operador@searchlink.dev`).

## Current Status

Implemented: MongoDB schema, Docker Compose, REST endpoints for alerts (`POST /api/alertas`, `GET /api/alertas`, `PATCH /api/alertas/{id}/estado`), geospatial user-lookup query.

Not yet implemented: Firebase FCM dispatch (stub in `AlertaService`), user/device registration endpoints, sighting creation endpoint, frontend, authentication.
