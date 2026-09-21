package com.senniaf.match.model.enums;

/**
 * Rendimiento académico general. Antes era un texto libre ("Bueno",
 * "Regular", etc.); se cierra a una escala fija para que sea comparable
 * entre perfiles y usable como criterio del motor de scoring.
 */
public enum RendimientoAcademico {
    EXCELENTE,
    BUENO,
    REGULAR,
    NECESITA_APOYO
}
