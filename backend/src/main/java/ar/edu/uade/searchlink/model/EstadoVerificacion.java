package ar.edu.uade.searchlink.model;

/**
 * Estado de moderación de un avistamiento reportado por la ciudadanía.
 *
 *  - PENDIENTE:  recién reportado, aún sin revisar (estado inicial, server-side).
 *  - VERIFICADO: un OPERADOR lo dio por válido.
 *  - DESCARTADO: un OPERADOR lo descartó (falso/erróneo/duplicado).
 *
 * El alta siempre nace PENDIENTE; sólo un OPERADOR transiciona a VERIFICADO o DESCARTADO
 * vía PATCH /api/avistamientos/{id}/estado.
 */
public enum EstadoVerificacion {
    PENDIENTE,
    VERIFICADO,
    DESCARTADO
}
