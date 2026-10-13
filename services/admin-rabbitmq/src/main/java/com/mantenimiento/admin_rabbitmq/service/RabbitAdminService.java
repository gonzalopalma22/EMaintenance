package com.mantenimiento.admin_rabbitmq.service;



import com.mantenimiento.admin_rabbitmq.dto.ExchangeRequest;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

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

    public boolean deleteExchange(String name) {
        return amqpAdmin.deleteExchange(name);
    }
} 
