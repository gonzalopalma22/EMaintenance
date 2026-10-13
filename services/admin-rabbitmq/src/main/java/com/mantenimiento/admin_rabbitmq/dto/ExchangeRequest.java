package com.mantenimiento.admin_rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExchangeRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+([.-][a-z0-9]+)*$",
                 message = "Solo minúsculas, números, puntos y guiones")
        String name,

        @NotBlank
        @Pattern(regexp = "^(topic|direct|fanout|headers)$",
                 message = "Tipo debe ser topic, direct, fanout o headers")
        String type,

        Boolean durable
) {}