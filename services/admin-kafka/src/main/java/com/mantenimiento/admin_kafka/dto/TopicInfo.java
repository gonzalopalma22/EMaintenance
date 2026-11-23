package com.mantenimiento.admin_kafka.dto;

public record TopicInfo(String name, int partitions, int replicationFactor, boolean internal) {
}