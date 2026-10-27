package com.mantenimiento.alertas.dto;

import java.util.UUID;

public record PrediccionEvent(
        UUID lecturaEventId,
        String equipoId,
        Double valor,
        Double score,
        Double delta,
        Boolean anomalia,
        String modeloVersion,
        String sensorCausante
) {}