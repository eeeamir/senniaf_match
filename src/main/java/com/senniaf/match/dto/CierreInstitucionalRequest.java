package com.senniaf.match.dto;
import jakarta.validation.constraints.*;
/** Constancia registrada por el Comité, sin atribuirle competencias de otra autoridad. */
public record CierreInstitucionalRequest(
 @NotBlank(message="Explica el motivo o resultado") @Size(min=10,max=2000,message="El motivo debe tener entre 10 y 2000 caracteres") String motivo,
 @AssertTrue(message="Confirma que el cierre cuenta con respaldo institucional") boolean confirmacionAutoridad) {}
