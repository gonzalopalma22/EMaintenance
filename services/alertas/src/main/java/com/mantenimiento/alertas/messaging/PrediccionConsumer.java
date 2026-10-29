package com.mantenimiento.alertas.messaging;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mantenimiento.alertas.config.AlertaProperties;
import com.mantenimiento.alertas.dto.PrediccionEvent;
import com.mantenimiento.alertas.model.Alerta;
import com.mantenimiento.alertas.repository.AlertaRepository;

@Component
public class PrediccionConsumer {

    private static final Logger log = LoggerFactory.getLogger(PrediccionConsumer.class);
    private static final String TIPO = "ANOMALIA";

    private final AlertaRepository repository;
    private final AlertaProperties props;

    public PrediccionConsumer(AlertaRepository repository, AlertaProperties props) {
        this.repository = repository;
        this.props = props;
    }

    @RabbitListener(queues = "mantenimiento.alertas-prediccion.queue")
    public void recibir(PrediccionEvent p) {
        if (p.equipoId() == null || p.lecturaEventId() == null) {
            throw new IllegalArgumentException("Prediccion incompleta: " + p);
        }
        if (!Boolean.TRUE.equals(p.anomalia())) {
            return;
        }
        if (repository.existsByEquipoIdAndTipoAndEstado(p.equipoId(), TIPO, "ABIERTA")) {
            return;
        }
        if (repository.existsByLecturaEventIdAndTipo(p.lecturaEventId(), TIPO)) {
            return;
        }
        Instant desde = Instant.now().minus(props.enfriamiento());
        if (repository.existsByEquipoIdAndTipoAndGeneradaEnAfter(p.equipoId(), TIPO, desde)) {
            return;
        }

        UUID id = UUID.randomUUID();
        double valor = p.valor() == null ? 0 : p.valor();
        double score = p.score() == null ? 0 : p.score();
        String sensor = "modelo-ml/" + (p.sensorCausante() == null ? "desconocido" : p.sensorCausante());

        repository.save(new Alerta(id, p.lecturaEventId(), p.equipoId(), sensor,
                TIPO, valor, score, "MEDIA", Instant.now()));

        log.warn("ALERTA ANOMALIA. equipo={} sensor={} valor={} score={}",
                p.equipoId(), p.sensorCausante(), valor, score);
    }
}