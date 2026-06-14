// Base URL del backend. Configurable por VITE_API_BASE_URL (ver .env.example);
// si no está definida, default al backend local de desarrollo.
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
