package com.mantenimiento.telemetria.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.mantenimiento.telemetria.dto.LecturaEvent;

@Component
public class LecturaProducer {

    private static final Logger log = LoggerFactory.getLogger(LecturaProducer.class);
    private static final String EXCHANGE = "mantenimiento.lecturas.exchange";
    private static final String ROUTING_KEY = "lectura.recibida";

    private final RabbitTemplate rabbitTemplate;

    public LecturaProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(LecturaEvent e) {
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, e);
        log.info("Lectura publicada. eventId={} equipo={}", e.eventId(), e.equipoId());
    }
}