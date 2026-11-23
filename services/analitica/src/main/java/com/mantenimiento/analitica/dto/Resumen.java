package com.mantenimiento.analitica.dto;

import java.time.Instant;

public record Resumen(
        String equipoId,
        String tipo,
        long ventanaSegundos,
        long muestras,
        double promedio,
        double minimo,
        double maximo,
        double ultimo,
        Instant calculadoEn) {
}