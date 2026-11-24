package com.mantenimiento.analitica.dto;

/** Lectura de un sensor tal como llega desde el tópico telemetria.lecturas. */
public record LecturaEvent(
        String eventId,
        String equipoId,
        String sensorId,
        String tipo,
        double valor,
        String timestamp) {
}