package com.mantenimiento.admin_kafka.dto;

import java.util.List;
import java.util.Map;

public record TopicDetail(
        String name,
        int partitions,
        int replicationFactor,
        List<Particion> particiones,
        Map<String, String> configs) {

    public record Particion(int partition, int leader, List<Integer> replicas, List<Integer> isr) {
    }
}