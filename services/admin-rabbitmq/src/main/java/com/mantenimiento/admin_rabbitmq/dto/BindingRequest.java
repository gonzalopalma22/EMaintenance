package com.mantenimiento.admin_rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BindingRequest(
        @NotBlank String exchange,
        @NotBlank String destination,

        @NotBlank
        @Pattern(regexp = "^(queue|exchange)$",
                 message = "destinationType debe ser queue o exchange")
        String destinationType,

        String routingKey
) {}