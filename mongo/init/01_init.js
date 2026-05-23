// 01_init.js — Inicialización de SearchLink en MongoDB
// Se ejecuta automáticamente al crear el contenedor por primera vez.
// Crea las colecciones, aplica los índices y carga datos de prueba mínimos.

db = db.getSiblingDB("searchlink");

// ─── Colecciones ─────────────────────────────────────────────────────────────

db.createCollection("usuarios");
db.createCollection("dispositivos");
db.createCollection("alertas");
db.createCollection("avistamientos");

// ─── Índices: usuarios ────────────────────────────────────────────────────────

// Índice geoespacial para consultas $nearSphere / $geoWithin
db.usuarios.createIndex({ "ubicacion": "2dsphere" });

// Email único para registro
db.usuarios.createIndex({ "email": 1 }, { unique: true });

// ─── Índices: dispositivos ────────────────────────────────────────────────────

// Buscar dispositivos de un usuario
db.dispositivos.createIndex({ "usuario_id": 1 });

// Token FCM único por dispositivo
db.dispositivos.createIndex({ "fcm_token": 1 }, { unique: true });

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

db.usuarios.insertOne({
  nombre: "Operador Prueba",
  email: "operador@searchlink.dev",
  password_hash: "$2b$10$placeholder_hash",
  rol: "operador",
  ubicacion: {
    type: "Point",
    coordinates: [-58.3816, -34.6037]  // Buenos Aires centro
  },
  activo: true,
  creado_en: new Date(),
  actualizado_en: new Date()
});

print("SearchLink: colecciones e índices creados correctamente.");
