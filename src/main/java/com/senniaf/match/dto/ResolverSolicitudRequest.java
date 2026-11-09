package com.senniaf.match.dto;

import jakarta.validation.constraints.*;

public record ResolverSolicitudRequest(@NotNull Boolean aprobar,
        @NotBlank @Size(min = 10, max = 2000) String motivo) {}
