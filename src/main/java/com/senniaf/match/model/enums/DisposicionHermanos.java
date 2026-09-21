package com.senniaf.match.model.enums;

/**
 * Disposición de la familia a recibir un grupo de hermanos. Reemplaza el
 * Sí/No binario original por opciones que capturan también la duda, tal
 * como lo pide el rediseño humanizado del Sprint 0 ("cuéntanos qué dudas
 * tendrías" en vez de un rechazo tajante).
 */
public enum DisposicionHermanos {
    DISPUESTA,
    DISPUESTA_CON_DUDAS,
    PREFIERE_UN_SOLO_NINO
}
