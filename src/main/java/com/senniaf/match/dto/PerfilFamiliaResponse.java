package com.senniaf.match.dto;

/**
 * Respuesta que recibe la familia al completar la entrevista de
 * idoneidad. Ejemplo:
 * {
 *   "mensaje": "Perfil registrado correctamente",
 *   "codigoCaso": "FAM-001",
 *   "perfilCompleto": true
 * }
 */
public class PerfilFamiliaResponse {

    private final String mensaje;
    private final String codigoCaso;
    private final boolean perfilCompleto;

    public PerfilFamiliaResponse(String mensaje, String codigoCaso, boolean perfilCompleto) {
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
