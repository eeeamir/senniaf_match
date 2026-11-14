package com.senniaf.match.dto;

import jakarta.validation.constraints.*;

public record RevisionInterrupcionRequest(
        @NotBlank(message="Explica la revisión realizada") @Size(min=10,max=2000,message="La revisión debe tener entre 10 y 2000 caracteres") String justificacion) {}
