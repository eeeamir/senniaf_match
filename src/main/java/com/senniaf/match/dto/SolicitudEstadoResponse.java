package com.senniaf.match.dto;

import com.senniaf.match.model.SolicitudEstadoCaso;
import java.time.LocalDateTime;

public record SolicitudEstadoResponse(Long id, String codigoCaso, SolicitudEstadoCaso.Tipo tipo,
        SolicitudEstadoCaso.Estado estado, String solicitante, @com.fasterxml.jackson.databind.annotation.JsonSerialize(using=com.senniaf.match.web.FechaConZonaSerializer.class) LocalDateTime fechaSolicitud,
        String motivo, String resueltoPor, @com.fasterxml.jackson.databind.annotation.JsonSerialize(using=com.senniaf.match.web.FechaConZonaSerializer.class) LocalDateTime fechaResolucion, String motivoResolucion, SolicitudEstadoCaso.Categoria categoria) {
    public static SolicitudEstadoResponse de(SolicitudEstadoCaso s) {
        return new SolicitudEstadoResponse(s.getId(), s.getExpediente().getCodigoCaso(), s.getTipo(),
                s.getEstado(), s.getSolicitante(), s.getFechaSolicitud(), s.getMotivo(),
                s.getResueltoPor(), s.getFechaResolucion(), s.getMotivoResolucion(), s.getCategoria());
    }
}
