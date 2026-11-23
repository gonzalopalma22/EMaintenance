package com.mantenimiento.admin_kafka.service;

public class KafkaAdminException extends RuntimeException {

    public KafkaAdminException(String mensaje) {
        super(mensaje);
    }

    public KafkaAdminException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}