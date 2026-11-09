package com.senniaf.match.dto;

import com.senniaf.match.model.SolicitudEstadoCaso;
import jakarta.validation.constraints.*;

public record SolicitudEstadoRequest(@NotNull SolicitudEstadoCaso.Tipo tipo,
        @NotBlank @Size(min = 10, max = 2000) String motivo, @NotNull SolicitudEstadoCaso.Categoria categoria) {
    public SolicitudEstadoRequest(SolicitudEstadoCaso.Tipo tipo, String motivo) { this(tipo, motivo, null); }
}
