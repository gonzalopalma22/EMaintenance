package com.mantenimiento.telemetria.messaging;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.mantenimiento.telemetria.dto.LecturaEvent;

/**
 * Publica cada lectura en Kafka para la analítica en streaming.
 * Es un envío "mejor esfuerzo": si Kafka falla, solo se registra en el log y
 * nunca se afecta el flujo principal (RabbitMQ y la base de datos).
 */
@Component
public class KafkaLecturaPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaLecturaPublisher.class);

    private final KafkaTemplate<String, String> template;
    private final String topic;

    public KafkaLecturaPublisher(KafkaTemplate<String, String> template,
                                 @Value("${telemetria.kafka.topic-lecturas}") String topic) {
        this.template = template;
        this.topic = topic;
    }

    public void publicar(LecturaEvent lectura) {
        try {
            // La clave es el equipo: sus lecturas caen en la misma partición y conservan el orden
            template.send(topic, lectura.equipoId(), aJson(lectura))
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            log.warn("No se pudo publicar la lectura en Kafka. eventId={} equipo={} motivo={}",
                                    lectura.eventId(), lectura.equipoId(), error.getMessage());
                        } else {
                            log.debug("Lectura publicada en Kafka. eventId={} particion={} offset={}",
                                    lectura.eventId(),
                                    resultado.getRecordMetadata().partition(),
                                    resultado.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Kafka no disponible, la lectura sigue su flujo normal. eventId={} motivo={}",
                    lectura.eventId(), e.getMessage());
        }
    }

    private static String aJson(LecturaEvent l) {
        return String.format(Locale.ROOT,
                "{\"eventId\":\"%s\",\"schemaVersion\":%d,\"equipoId\":\"%s\",\"sensorId\":\"%s\","
                        + "\"tipo\":\"%s\",\"valor\":%s,\"timestamp\":\"%s\"}",
                l.eventId(), l.schemaVersion(), escapar(l.equipoId()), escapar(l.sensorId()),
                escapar(l.tipo()), l.valor(), l.timestamp());
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}