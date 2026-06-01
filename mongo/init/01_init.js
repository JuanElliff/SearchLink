// 01_init.js — Inicialización de SearchLink en MongoDB
// Se ejecuta automáticamente al crear el contenedor por primera vez.
// Crea las colecciones, aplica los índices y carga datos de prueba mínimos.

db = db.getSiblingDB("searchlink");

// ─── Colecciones ─────────────────────────────────────────────────────────────

db.createCollection("usuarios");
db.createCollection("alertas");
db.createCollection("avistamientos");

// ─── Índices: usuarios ────────────────────────────────────────────────────────

// Email único para registro
db.usuarios.createIndex({ "email": 1 }, { unique: true });

// Ubicación precargada (cargada al registrarse, siempre presente):
// índice 2dsphere normal — soporta el path de despacho push actual.
db.usuarios.createIndex({ "ubicacion_precargada": "2dsphere" });

// Ubicación actual (GPS con consentimiento, opcional):
// índice 2dsphere sparse. Los 2dsphere ya ignoran documentos sin el campo,
// pero declaramos sparse para explicitar la semántica de campo opcional.
db.usuarios.createIndex(
  { "ubicacion_actual": "2dsphere" },
  { sparse: true }
);

// Tokens FCM embebidos en usuarios.dispositivos[].fcm_token.
// Sparse + único: un token no puede pertenecer a dos usuarios, pero un usuario
// puede no tener ningún dispositivo todavía.
db.usuarios.createIndex(
  { "dispositivos.fcm_token": 1 },
  { unique: true, sparse: true }
);

// ─── Índices: alertas ─────────────────────────────────────────────────────────

// Índice geoespacial para consultas de proximidad al publicar una alerta
db.alertas.createIndex({ "ubicacion": "2dsphere" });

// Filtrar alertas por estado (activa / resuelta / cancelada)
db.alertas.createIndex({ "estado": 1 });

// TTL: MongoDB elimina automáticamente el documento cuando expira_en <= now
db.alertas.createIndex({ "expira_en": 1 }, { expireAfterSeconds: 0 });

// Orden cronológico inverso para el listado general
db.alertas.createIndex({ "creada_en": -1 });

// ─── Índices: avistamientos ───────────────────────────────────────────────────

// Índice geoespacial para mapear avistamientos en el mapa
db.avistamientos.createIndex({ "ubicacion": "2dsphere" });

// Buscar todos los avistamientos de una alerta
db.avistamientos.createIndex({ "alerta_id": 1 });

// Orden cronológico inverso
db.avistamientos.createIndex({ "creado_en": -1 });

// ─── Datos de prueba ──────────────────────────────────────────────────────────
// Seeds elegidos para cubrir ambas ramas del coalesce de ubicación:
//   - Operador y ciudadano "Belgrano" tienen SOLO ubicacion_precargada.
//   - Ciudadano "Caballito" tiene precargada Y actual (rama GPS con consentimiento).

const ahora = new Date();

db.usuarios.insertMany([
  {
    nombre: "Operador Prueba",
    email: "operador@searchlink.dev",
    password_hash: "$2b$10$placeholder_hash",
    rol: "operador",
    ubicacion_precargada: {
      type: "Point",
      coordinates: [-58.3816, -34.6037]  // BA centro (Plaza de Mayo)
    },
    // ubicacion_actual ausente: sin consentimiento GPS — rama "precargada" del coalesce
    activo: true,
    dispositivos: [],
    creado_en: ahora,
    actualizado_en: ahora
  },
  {
    nombre: "Ciudadano Belgrano (sin GPS)",
    email: "belgrano@searchlink.dev",
    password_hash: "$2b$10$placeholder_hash",
    rol: "ciudadano",
    ubicacion_precargada: {
      type: "Point",
      coordinates: [-58.4566, -34.5627]  // Belgrano
    },
    // ubicacion_actual ausente — rama "precargada"
    activo: true,
    dispositivos: [],
    creado_en: ahora,
    actualizado_en: ahora
  },
  {
    nombre: "Ciudadano Caballito (con GPS)",
    email: "caballito@searchlink.dev",
    password_hash: "$2b$10$placeholder_hash",
    rol: "ciudadano",
    ubicacion_precargada: {
      type: "Point",
      coordinates: [-58.4400, -34.6190]  // Caballito (domicilio)
    },
    ubicacion_actual: {
      type: "Point",
      coordinates: [-58.4100, -34.6080]  // Almagro (donde está ahora) — rama "actual"
    },
    activo: true,
    dispositivos: [],
    creado_en: ahora,
    actualizado_en: ahora
  }
]);

print("SearchLink: colecciones e índices creados correctamente.");
