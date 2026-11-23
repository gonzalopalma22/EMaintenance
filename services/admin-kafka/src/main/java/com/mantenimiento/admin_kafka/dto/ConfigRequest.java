package com.mantenimiento.admin_kafka.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

/** Configuraciones a fijar. Un valor null restablece esa configuración a su valor por defecto. */
public record ConfigRequest(@NotEmpty Map<String, String> configs) {
}