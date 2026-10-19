package com.mantenimiento.alertas.messaging;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.mantenimiento.alertas.dto.AlertaEvent;
import com.mantenimiento.alertas.dto.LecturaEvent;
import com.mantenimiento.alertas.model.Alerta;
import com.mantenimiento.alertas.repository.AlertaRepository;

@Component
public class AlertaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertaConsumer.class);
    private static final String EXCHANGE = "mantenimiento.eventos.exchange";
    private static final String RK = "alerta.generada";
    private static final double UMBRAL_TEMPERATURA = 100.0;

    private final RabbitTemplate rabbitTemplate;
    private final AlertaRepository repository;

    public AlertaConsumer(RabbitTemplate rabbitTemplate, AlertaRepository repository) {
        this.rabbitTemplate = rabbitTemplate;
        this.repository = repository;
    }

    @RabbitListener(queues = "mantenimiento.alertas-entrada.queue")
    public void recibir(LecturaEvent e) {
        if (!"temperatura".equals(e.tipo()) || e.valor() == null
                || e.valor() <= UMBRAL_TEMPERATURA) {
            log.info("Lectura normal. eventId={} valor={}", e.eventId(), e.valor());
            return;
        }
        if (repository.existsByLecturaEventId(e.eventId())) {
            log.info("Alerta duplicada ignorada. lecturaEventId={}", e.eventId());
            return;
        }
        String severidad = e.valor() > 150 ? "CRITICA" : "ALTA";
        UUID alertaId = UUID.randomUUID();
        Instant ahora = Instant.now();

        repository.save(new Alerta(alertaId, e.eventId(), e.equipoId(), e.sensorId(),
                e.tipo(), e.valor(), UMBRAL_TEMPERATURA, severidad, ahora));

        rabbitTemplate.convertAndSend(EXCHANGE, RK, new AlertaEvent(alertaId, e.eventId(),
                e.equipoId(), e.sensorId(), e.tipo(), e.valor(),
                UMBRAL_TEMPERATURA, severidad, ahora));

        log.warn("ALERTA generada. alertaId={} equipo={} valor={} severidad={}",
                alertaId, e.equipoId(), e.valor(), severidad);
    }
}