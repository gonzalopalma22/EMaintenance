package com.mantenimiento.admin_rabbitmq.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.mantenimiento.admin_rabbitmq.dto.ExchangeRequest;
import com.mantenimiento.admin_rabbitmq.dto.QueueRequest;
import com.mantenimiento.admin_rabbitmq.exception.ConflictException;
import com.mantenimiento.admin_rabbitmq.exception.ForbiddenOperationException;
import com.mantenimiento.admin_rabbitmq.exception.NotFoundException;

@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitTemplate rabbitTemplate) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
    }

    // ---------- Exchanges ----------

    public void createExchange(ExchangeRequest req) {
        boolean durable = req.durable() == null || req.durable();
        Exchange exchange = switch (req.type()) {
            case "topic"  -> new TopicExchange(req.name(), durable, false);
            case "direct" -> new DirectExchange(req.name(), durable, false);
            case "fanout" -> new FanoutExchange(req.name(), durable, false);
            default       -> new HeadersExchange(req.name(), durable, false);
        };
        amqpAdmin.declareExchange(exchange);
    }

    public void deleteExchange(String name) {
        if (name.startsWith("amq.")) {
            throw new ForbiddenOperationException(
                    "No se pueden eliminar exchanges del sistema: " + name);
        }
        if (!exchangeExists(name)) {
            throw new NotFoundException("El exchange no existe: " + name);
        }
        amqpAdmin.deleteExchange(name);
    }

    private boolean exchangeExists(String name) {
        return Boolean.TRUE.equals(rabbitTemplate.execute(channel -> {
            try {
                channel.exchangeDeclarePassive(name);
                return true;
            } catch (IOException e) {
                return false; // 404 del broker
            }
        }));
    }

    // ---------- Colas ----------

    public void createQueue(QueueRequest req) {
        boolean durable = req.durable() == null || req.durable();
        boolean withDlq = req.withDlq() == null || req.withDlq();
        String base = req.name().substring(0, req.name().length() - ".queue".length());

        Map<String, Object> args = new HashMap<>();
        if (req.messageTtlMs() != null) {
            args.put("x-message-ttl", req.messageTtlMs());
        }

        if (withDlq) {
            String dlx = base + ".dlx";
            String dlq = base + ".dlq";
            String dlRoutingKey = base + ".dead";

            amqpAdmin.declareExchange(new DirectExchange(dlx, true, false));
            amqpAdmin.declareQueue(new Queue(dlq, true));
            amqpAdmin.declareBinding(
                    BindingBuilder.bind(new Queue(dlq, true))
                            .to(new DirectExchange(dlx, true, false))
                            .with(dlRoutingKey));

            args.put("x-dead-letter-exchange", dlx);
            args.put("x-dead-letter-routing-key", dlRoutingKey);
        }

        amqpAdmin.declareQueue(new Queue(req.name(), durable, false, false, args));
    }

    public void deleteQueue(String name, boolean force) {
        if (name.startsWith("amq.")) {
            throw new ForbiddenOperationException(
                    "No se pueden eliminar colas del sistema: " + name);
        }
        var props = amqpAdmin.getQueueProperties(name);
        if (props == null) {
            throw new NotFoundException("La cola no existe: " + name);
        }
        Number count = (Number) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        long messages = count == null ? 0L : count.longValue();
        if (messages > 0 && !force) {
            throw new ConflictException(
                    "La cola tiene " + messages + " mensajes. Usa ?force=true para eliminarla");
        }
        amqpAdmin.deleteQueue(name);
    }
}