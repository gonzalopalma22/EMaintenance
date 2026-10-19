package com.mantenimiento.ordenes_trabajo.messaging;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mantenimiento.ordenes_trabajo.dto.AlertaEvent;
import com.mantenimiento.ordenes_trabajo.model.OrdenTrabajo;
import com.mantenimiento.ordenes_trabajo.repository.OrdenTrabajoRepository;

@Component
public class OrdenConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrdenConsumer.class);

    private final OrdenTrabajoRepository repository;

    public OrdenConsumer(OrdenTrabajoRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "mantenimiento.ordenes-entrada.queue")
    public void recibir(AlertaEvent a) {
        if (a.alertaId() == null || a.equipoId() == null || a.severidad() == null) {
            throw new IllegalArgumentException("Alerta incompleta: " + a);
        }
        if (repository.existsByAlertaId(a.alertaId())) {
            log.info("Orden duplicada ignorada. alertaId={}", a.alertaId());
            return;
        }
        String descripcion = String.format(
                "Revisar %s en %s: %s %.1f supera el umbral %.1f",
                a.sensorId(), a.equipoId(), a.tipo(), a.valor(), a.umbral());

        OrdenTrabajo orden = new OrdenTrabajo(UUID.randomUUID(), a.alertaId(),
                a.equipoId(), descripcion, a.severidad(), Instant.now());
        repository.save(orden);

        log.warn("ORDEN creada. ordenId={} alertaId={} equipo={} prioridad={}",
                orden.getId(), a.alertaId(), a.equipoId(), a.severidad());
    }
}