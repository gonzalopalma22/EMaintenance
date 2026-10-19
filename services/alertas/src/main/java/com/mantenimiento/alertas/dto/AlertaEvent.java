package com.mantenimiento.alertas.dto;

import java.time.Instant;
import java.util.UUID;

public record AlertaEvent(
        UUID alertaId,
        UUID lecturaEventId,
        String equipoId,
        String sensorId,
        String tipo,
        Double valor,
        Double umbral,
        String severidad,
        Instant generadaEn
) {}