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
    private final boolean reevaluacionPendiente;
    private final int numeroReevaluaciones;

    public PerfilFamiliaResponse(String mensaje, String codigoCaso, boolean perfilCompleto) {
        this(mensaje, codigoCaso, perfilCompleto, false, 0);
    }

    public PerfilFamiliaResponse(String mensaje, String codigoCaso, boolean perfilCompleto, boolean pendiente, int numero) {
        this.mensaje = mensaje;
        this.codigoCaso = codigoCaso;
        this.perfilCompleto = perfilCompleto;
        this.reevaluacionPendiente = pendiente;
        this.numeroReevaluaciones = numero;
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
    public boolean isReevaluacionPendiente() { return reevaluacionPendiente; }
    public int getNumeroReevaluaciones() { return numeroReevaluaciones; }
    public boolean isRequiereRevisionTecnica() { return numeroReevaluaciones >= 3; }
}
