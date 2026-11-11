package com.senniaf.match.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReenviarCodigoRequest(
        @JsonAlias("cedula") @NotBlank(message="Indica la cuenta para reenviar el código")
        @Size(max=40,message="Revisa el identificador de la cuenta") String identificador) {}
