package com.mantenimiento.admin_kafka.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PartitionsRequest(@NotNull @Min(1) @Max(50) Integer partitions) {
}