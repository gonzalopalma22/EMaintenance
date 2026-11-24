package com.mantenimiento.admin_kafka.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.mantenimiento.admin_kafka.dto.TopicRequest;

/**
 * Las protecciones sobre los nombres se validan antes de hablar con Kafka,
 * por eso estas pruebas no necesitan un cluster.
 */
class KafkaAdminServiceTest {

    private final KafkaAdminService servicio = new KafkaAdminService(null);

    @Test
    void noPermiteCrearTopicosInternos() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearTopic(new TopicRequest("__consumer_offsets", 1, 1, null)));
    }

    @Test
    void noPermiteEliminarTopicosInternos() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.eliminarTopic("__consumer_offsets"));
    }

    @Test
    void noPermiteModificarTopicosInternos() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.aumentarParticiones("__transaction_state", 10));
    }

    @Test
    void rechazaLosNombresPuntoYPuntoPunto() {
        assertThrows(IllegalArgumentException.class, () -> servicio.eliminarTopic("."));
        assertThrows(IllegalArgumentException.class, () -> servicio.eliminarTopic(".."));
    }
}