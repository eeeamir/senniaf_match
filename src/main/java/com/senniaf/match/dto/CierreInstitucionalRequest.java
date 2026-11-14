package com.senniaf.match.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
/** Constancia de una actuación externa; no sustituye la competencia de la autoridad. */
public record CierreInstitucionalRequest(
 @NotNull(message="Indica la fecha del cierre") LocalDate fechaCierre,
 @NotBlank(message="Indica la referencia institucional") @Size(min=5,max=200,message="La referencia debe tener entre 5 y 200 caracteres") String referenciaInstitucional,
 @NotBlank(message="Indica la autoridad responsable") @Size(min=3,max=200,message="La autoridad debe tener entre 3 y 200 caracteres") String autoridadResponsable,
 @NotBlank(message="Explica el motivo o resultado") @Size(min=10,max=2000,message="El motivo debe tener entre 10 y 2000 caracteres") String motivo,
 @AssertTrue(message="Confirma que cuentas con la actuación de la autoridad competente") boolean confirmacionAutoridad) {}
