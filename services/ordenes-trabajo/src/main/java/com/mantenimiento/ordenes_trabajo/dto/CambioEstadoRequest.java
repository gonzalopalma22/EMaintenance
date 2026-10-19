package com.mantenimiento.ordenes_trabajo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambioEstadoRequest(
        @NotBlank
        @Pattern(regexp = "^(ASIGNADA|EN_CURSO|REALIZADA|CERRADA)$",
                message = "Estado debe ser ASIGNADA, EN_CURSO, REALIZADA o CERRADA")
        String estado,
        String asignadoA
) {}