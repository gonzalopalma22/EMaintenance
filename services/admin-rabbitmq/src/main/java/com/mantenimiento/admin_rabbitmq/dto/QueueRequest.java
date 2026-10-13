package com.mantenimiento.admin_rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record QueueRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+([.-][a-z0-9]+)*\\.queue$",
                 message = "Debe terminar en .queue, solo minúsculas, números, puntos y guiones")
        String name,

        Boolean durable,

        Boolean withDlq,

        Integer messageTtlMs
) {}