package com.mantenimiento.alertas.messaging;

import java.time.Instant;
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

    @RabbitListener(queues = "mantenimiento.alertas-entrada.dlq.audit.queue",
            containerFactory = "dlqListenerFactory")
    public void registrar(Message message) {
        Map<String, Object> h = message.getMessageProperties().getHeaders();
        String motivo = "desconocido";
        String cola = "desconocida";
        long intentos = 0;
        if (h.get("x-death") instanceof List<?> l && !l.isEmpty()
                && l.get(0) instanceof Map<?, ?> d) {
            motivo = String.valueOf(d.get("reason"));
            cola = String.valueOf(d.get("queue"));
            intentos = d.get("count") instanceof Number n ? n.longValue() : 0;
        }
        log.error("DLQ | messageId={} cola={} motivo={} intentos={} fecha={}",
                message.getMessageProperties().getMessageId(),
                cola, motivo, intentos, Instant.now());
    }

    @RabbitListener(queues = "mantenimiento.alertas-prediccion.dlq.audit.queue",
            containerFactory = "dlqListenerFactory")
    public void registrarPrediccion(Message message) {
        registrar(message);
    }
}