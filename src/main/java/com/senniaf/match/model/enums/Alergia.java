package com.senniaf.match.model.enums;

/**
 * Alergias conocidas del niño o niña (selección múltiple). Se exige
 * explícitamente "NINGUNA_CONOCIDA" en vez de dejar la lista vacía, para
 * que un campo sin marcar signifique "no evaluado" y no se confunda con
 * "sin alergias" (dato médico relevante para la familia candidata).
 */
public enum Alergia {
    NINGUNA_CONOCIDA,
    ALIMENTARIA,
    MEDICAMENTOS,
    AMBIENTAL_POLEN,
    PICADURA_INSECTOS,
    OTRA
}
