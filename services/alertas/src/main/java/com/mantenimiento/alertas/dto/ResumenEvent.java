package com.mantenimiento.alertas.dto;

import java.time.Instant;

/** Resumen por equipo y sensor publicado por analitica en analitica.resumen (contrato versión 1). */
public record ResumenEvent(
        Integer schemaVersion,
        String equipoId,
        String tipo,
        Long ventanaSegundos,
        Long muestras,
        Double promedio,
        Double minimo,
        Double maximo,
        Double ultimo,
        Instant calculadoEn
) {}