package com.senniaf.match.dto;

import jakarta.validation.constraints.*;
import com.senniaf.match.model.enums.MotivoInterrupcion;

public record InterrupcionRequest(
        @NotNull(message="Selecciona el motivo de la interrupción") MotivoInterrupcion motivo,
        @NotBlank(message="Describe lo ocurrido") @Size(min=10,max=2000,message="La descripción debe tener entre 10 y 2000 caracteres") String justificacion) {}
