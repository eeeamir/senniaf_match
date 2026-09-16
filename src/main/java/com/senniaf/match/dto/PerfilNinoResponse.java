package com.senniaf.match.dto;

/**
 * Respuesta que recibe el trabajador social al registrar un perfil.
 * Ejemplo: { "mensaje": "Perfil registrado correctamente", "codigoCaso": "NNA-001", "perfilCompleto": true }
 */
public class PerfilNinoResponse {

    private final String mensaje;
    private final String codigoCaso;
    private final boolean perfilCompleto;

    public PerfilNinoResponse(String mensaje, String codigoCaso, boolean perfilCompleto) {
        this.mensaje = mensaje;
        this.codigoCaso = codigoCaso;
        this.perfilCompleto = perfilCompleto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getCodigoCaso() {
        return codigoCaso;
    }

    public boolean isPerfilCompleto() {
        return perfilCompleto;
    }
}
