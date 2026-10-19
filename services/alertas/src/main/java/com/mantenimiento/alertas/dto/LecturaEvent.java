package com.mantenimiento.alertas.dto;

import java.time.Instant;
import java.util.UUID;

public record LecturaEvent(
        UUID eventId,
        Integer schemaVersion,
        String equipoId,
        String sensorId,
        String tipo,
        Double valor,
        Instant timestamp
) {}