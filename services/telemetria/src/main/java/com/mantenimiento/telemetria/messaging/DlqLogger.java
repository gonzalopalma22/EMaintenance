package com.mantenimiento.telemetria.messaging;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DlqLogger {

    private static final Logger log = LoggerFactory.getLogger(DlqLogger.class);

    @RabbitListener(queues = "mantenimiento.lecturas.dlq.audit.queue")
    public void registrar(Message message) {
        Map<String, Object> h = message.getMessageProperties().getHeaders();
        Object death = h.get("x-death");
        String motivo = "desconocido";
        long intentos = 0;
        String colaOrigen = "desconocida";
        if (death instanceof List<?> lista && !lista.isEmpty()
                && lista.get(0) instanceof Map<?, ?> d) {
            motivo = String.valueOf(d.get("reason"));
            intentos = d.get("count") instanceof Number n ? n.longValue() : 0;
            colaOrigen = String.valueOf(d.get("queue"));
        }
        log.error("DLQ | messageId={} cola={} motivo={} intentos={} fecha={} exception={}",
                message.getMessageProperties().getMessageId(),
                colaOrigen, motivo, intentos,
                java.time.Instant.now(),
                h.get("x-exception-message"));
    }
}