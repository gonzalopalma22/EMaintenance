package com.mantenimiento.telemetria.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mantenimiento.telemetria.dto.LecturaEvent;
import com.mantenimiento.telemetria.exception.LecturaInvalidaException;
import com.mantenimiento.telemetria.model.Lectura;
import com.mantenimiento.telemetria.repository.LecturaRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Component
public class LecturaConsumer {

    private static final Logger log = LoggerFactory.getLogger(LecturaConsumer.class);

    private final LecturaRepository repository;
    private final Validator validator;

    public LecturaConsumer(LecturaRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @RabbitListener(queues = "mantenimiento.lecturas.queue")
    public void recibir(LecturaEvent e) {
        var errores = validator.validate(e);
        if (!errores.isEmpty()) {
            String motivo = errores.stream()
                    .map(ConstraintViolation::getMessage)
                    .reduce((a, b) -> a + "; " + b).orElse("invalida");
            log.warn("Lectura rechazada -> DLQ. eventId={} motivo={}", e.eventId(), motivo);
            throw new LecturaInvalidaException(motivo);
        }
        if (repository.existsById(e.eventId())) {
            log.info("Lectura duplicada ignorada. eventId={}", e.eventId());
            return;
        }
        repository.save(new Lectura(e.eventId(), e.equipoId(), e.sensorId(),
                e.tipo(), e.valor(), e.timestamp()));
        log.info("Lectura guardada. eventId={} equipo={} tipo={} valor={}",
                e.eventId(), e.equipoId(), e.tipo(), e.valor());
    }
}