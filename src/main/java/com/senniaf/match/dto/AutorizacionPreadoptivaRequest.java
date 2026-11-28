package com.senniaf.match.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Constancia de un acto ya emitido. El servidor registra el momento de la actuación. */
public record AutorizacionPreadoptivaRequest(
        @NotNull @AssertTrue(message = "Confirma la existencia de la autorización emitida por SENNIAF") Boolean confirmacion) {

    @JsonAnySetter
    public void rechazarCampoAdicional(String campo, Object valor) {
        throw new IllegalArgumentException("El registro de autorización solo admite la confirmación institucional");
    }
}
