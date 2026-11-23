package com.mantenimiento.admin_kafka.dto;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TopicRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,249}$",
                 message = "El nombre solo admite letras, números, punto, guion y guion bajo (máx. 249)")
        String name,
        @NotNull @Min(1) @Max(50) Integer partitions,
        @NotNull @Min(1) @Max(10) Integer replicationFactor,
        Map<String, String> configs) {
}