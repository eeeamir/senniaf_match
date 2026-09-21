package com.senniaf.match.model.enums;

/**
 * Condiciones de salud o desarrollo conocidas (selección múltiple). Igual
 * que con las alergias, "NINGUNA_CONOCIDA" es una opción explícita: el
 * Comité necesita saber que el campo fue evaluado y no encontró nada, no
 * solo que quedó vacío.
 */
public enum CondicionSalud {
    NINGUNA_CONOCIDA,
    ASMA,
    DIABETES,
    EPILEPSIA,
    TDAH,
    TEA,
    DISCAPACIDAD_FISICA,
    DISCAPACIDAD_INTELECTUAL,
    DISCAPACIDAD_VISUAL,
    DISCAPACIDAD_AUDITIVA,
    OTRA
}
