package com.mantenimiento.admin_rabbitmq.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.mantenimiento.admin_rabbitmq.exception.ConflictException;
import com.mantenimiento.admin_rabbitmq.exception.ForbiddenOperationException;

@Service
public class DlqService {

    private static final Logger log = LoggerFactory.getLogger(DlqService.class);

    private final RabbitTemplate rabbitTemplate;

    public DlqService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    private void validar(String cola) {
        if (!cola.endsWith(".dlq")) {
            throw new ForbiddenOperationException("Solo se permiten colas .dlq: " + cola);
        }
    }

    /** Lee sin consumir: toma el mensaje y lo devuelve a la cola (requeue) con una conexión transaccional. */
    public List<Map<String, Object>> ver(String cola, int limite) {
        validar(cola);
        List<Map<String, Object>> resultado = new ArrayList<>();
        rabbitTemplate.execute(channel -> {
            for (int i = 0; i < limite; i++) {
                var resp = channel.basicGet(cola, false);
                if (resp == null) break;
                resultado.add(Map.of(
                        "cuerpo", new String(resp.getBody()),
                        "cabeceras", String.valueOf(resp.getProps().getHeaders())));
                // se devuelven todos al final para no volver a leer el mismo
            }
            channel.basicRecover(true);   // reencola todo lo no confirmado
            return null;
        });
        return resultado;
    }

    public int reprocesar(String cola, int cantidad) {
        validar(cola);
        int movidos = 0;
        for (int i = 0; i < cantidad; i++) {
            Message m = rabbitTemplate.receive(cola);
            if (m == null) break;
            String destino = colaOriginal(m.getMessageProperties());
            if (destino == null) {
                // no sabemos adónde volver: se devuelve a la DLQ y se corta
                rabbitTemplate.send("", cola, m);
                throw new ConflictException("Mensaje sin x-death, no se puede reprocesar");
            }
            m.getMessageProperties().getHeaders().remove("x-death");
            rabbitTemplate.send("", destino, m);
            movidos++;
            log.info("DLQ reprocesado. dlq={} destino={}", cola, destino);
        }
        return movidos;
    }

    private String colaOriginal(MessageProperties p) {
        Object death = p.getHeaders().get("x-death");
        if (death instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Map<?, ?> d) {
            return String.valueOf(d.get("queue"));
        }
        return null;
    }
}