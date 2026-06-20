package ar.edu.uade.searchlink.model;

/**
 * Roles de autorización del sistema. Modelo de TRES roles con separación de
 * responsabilidades:
 *
 *  - ADMIN:    dueño/administrador de la aplicación. Gestiona usuarios (crea operadores
 *              y otros admins). NO emite alertas ni modera avistamientos.
 *  - OPERADOR: autoridad operativa. Emite y gestiona alertas; modera avistamientos.
 *  - ESTANDAR: ciudadanía. Reporta avistamientos y recibe notificaciones push.
 *
 * El registro público SIEMPRE asigna ESTANDAR (ver UsuarioService): un cliente no puede
 * auto-asignarse un rol privilegiado. ADMIN y OPERADOR se crean sólo por el seed de Mongo
 * o por los endpoints protegidos POST /api/usuarios/admin y POST /api/usuarios/operador
 * (ambos sólo ADMIN).
 */
public enum RolUsuario {
    ADMIN,
    OPERADOR,
    ESTANDAR
}
