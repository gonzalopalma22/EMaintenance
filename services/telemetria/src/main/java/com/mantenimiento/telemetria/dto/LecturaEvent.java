package com.mantenimiento.telemetria.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LecturaEvent(
        @NotNull UUID eventId,
        @NotNull Integer schemaVersion,
        @NotBlank String equipoId,
        @NotBlank String sensorId,
        @NotBlank String tipo,              
        @NotNull @DecimalMin("-100.0") @DecimalMax("2000.0") Double valor,
        @NotNull Instant timestamp
) {}