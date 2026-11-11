package com.senniaf.match.web;

/** Error de entrada con un campo concreto; nunca contiene valores ni secretos del usuario. */
public class CampoInvalidoException extends IllegalArgumentException {
    private final String campo;
    private final boolean conflicto;
    public CampoInvalidoException(String campo, String mensaje, boolean conflicto) {
        super(mensaje); this.campo = campo; this.conflicto = conflicto;
    }
    public String getCampo() { return campo; }
    public boolean isConflicto() { return conflicto; }
}
