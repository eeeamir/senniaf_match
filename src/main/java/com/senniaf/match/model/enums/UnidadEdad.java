package com.senniaf.match.model.enums;

/** Conserva la precisión declarada: los años no se convierten en meses supuestos. */
public enum UnidadEdad {
    ANIOS, MESES;

    public boolean admite(Integer edad) {
        return edad != null && (this == MESES ? edad >= 0 && edad <= 11 : edad >= 1 && edad <= 17);
    }
}
