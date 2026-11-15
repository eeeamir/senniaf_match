package com.senniaf.match.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Constancia de un acto ya emitido; el portal no lo concede. */
public record AutorizacionPreadoptivaRequest(
 @NotBlank @Size(min=5,max=200) String referencia,
 @NotNull LocalDate fechaEmision,
 LocalDate fechaInicioConvivencia,
 @NotNull @AssertTrue(message="Confirma la existencia de la autorización emitida por SENNIAF") Boolean confirmacion) {}
