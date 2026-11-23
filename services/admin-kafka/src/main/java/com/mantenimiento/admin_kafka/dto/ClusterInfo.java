package com.mantenimiento.admin_kafka.dto;

import java.util.List;

public record ClusterInfo(String clusterId, int controller, List<Broker> brokers) {

    public record Broker(int id, String host, int port) {
    }
}