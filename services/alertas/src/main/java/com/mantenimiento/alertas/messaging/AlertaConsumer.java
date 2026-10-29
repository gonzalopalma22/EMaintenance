package com.mantenimiento.alertas.messaging;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mantenimiento.alertas.config.AlertaProperties;
import com.mantenimiento.alertas.dto.LecturaEvent;
import com.mantenimiento.alertas.model.Alerta;
import com.mantenimiento.alertas.repository.AlertaRepository;

@Component
public class AlertaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertaConsumer.class);

    private final AlertaRepository repository;
    private final AlertaProperties props;

    public AlertaConsumer(AlertaRepository repository, AlertaProperties props) {
        this.repository = repository;
        this.props = props;
    }

    @RabbitListener(queues = "mantenimiento.alertas-entrada.queue")
    public void recibir(LecturaEvent e) {
        if (e.tipo() == null || e.valor() == null) {
            log.warn("Lectura incompleta ignorada. eventId={}", e.eventId());
            return;
        }

        AlertaProperties.Regla regla = props.reglas().get(e.tipo().toLowerCase(Locale.ROOT));
        if (regla == null) {
            log.info("Sin regla para tipo={}. eventId={}", e.tipo(), e.eventId());
            return;
        }

        if (e.valor() <= regla.umbral()) {
            log.info("Lectura normal. tipo={} eventId={} valor={}", e.tipo(), e.eventId(), e.valor());
            return;
        }
        if (repository.existsByLecturaEventIdAndTipo(e.eventId(), e.tipo())) {
            log.info("Alerta duplicada ignorada. lecturaEventId={} tipo={}", e.eventId(), e.tipo());
            return;
        }
        if (repository.existsByEquipoIdAndTipoAndEstado(e.equipoId(), e.tipo(), "ABIERTA")) {
            log.info("Ya hay alerta de {} abierta. equipo={}", e.tipo(), e.equipoId());
            return;
        }
        Instant desde = Instant.now().minus(props.enfriamiento());
        if (repository.existsByEquipoIdAndTipoAndGeneradaEnAfter(e.equipoId(), e.tipo(), desde)) {
            log.info("Alerta de {} en enfriamiento. equipo={}", e.tipo(), e.equipoId());
            return;
        }

        String severidad = e.valor() > regla.critico() ? "CRITICA" : "ALTA";
        UUID alertaId = UUID.randomUUID();

        repository.save(new Alerta(alertaId, e.eventId(), e.equipoId(), e.sensorId(),
                e.tipo(), e.valor(), regla.umbral(), severidad, Instant.now()));

        log.warn("ALERTA generada. alertaId={} equipo={} tipo={} valor={} severidad={}",
                alertaId, e.equipoId(), e.tipo(), e.valor(), severidad);
    }
}