package com.senniaf.match.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record InterrupcionRequest(
        @NotNull(message="Indica la fecha de la interrupción") LocalDate fechaInterrupcion,
        @NotBlank(message="Indica la referencia institucional de la convivencia") @Size(min=5,max=200,message="La referencia debe tener entre 5 y 200 caracteres") String referenciaActuacion,
        @NotBlank(message="Explica el motivo de la interrupción") @Size(min=10,max=2000,message="La justificación debe tener entre 10 y 2000 caracteres") String justificacion) {}
