package com.senniaf.match.dto;

import java.time.LocalDateTime;

/** Respuesta privada de la familia, sin datos de niños, motivos ni rankings. */
public class PerfilFamiliaResponse {
    private final String mensaje;
    private final String codigoCaso;
    private final boolean perfilCompleto;
    private final String estadoSolicitud;
    private final LocalDateTime fechaDecisionComite;

    public PerfilFamiliaResponse(String mensaje, String codigoCaso, boolean perfilCompleto) {
        this(mensaje, codigoCaso, perfilCompleto, perfilCompleto ? "EN_ESPERA" : "INCOMPLETA", null);
    }

    public PerfilFamiliaResponse(String mensaje, String codigoCaso, boolean perfilCompleto,
                                String estadoSolicitud, LocalDateTime fechaDecisionComite) {
        this.mensaje = mensaje;
        this.codigoCaso = codigoCaso;
        this.perfilCompleto = perfilCompleto;
        this.estadoSolicitud = estadoSolicitud;
        this.fechaDecisionComite = fechaDecisionComite;
    }

    public String getMensaje() { return mensaje; }
    public String getCodigoCaso() { return codigoCaso; }
    public boolean isPerfilCompleto() { return perfilCompleto; }
    public String getEstadoSolicitud() { return estadoSolicitud; }
    public LocalDateTime getFechaDecisionComite() { return fechaDecisionComite; }
}
