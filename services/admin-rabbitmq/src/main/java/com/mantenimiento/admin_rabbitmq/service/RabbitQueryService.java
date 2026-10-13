package com.mantenimiento.admin_rabbitmq.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RabbitQueryService {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {};

    private static final String VHOST = "/";

    private final RestClient client;

    public RabbitQueryService(RestClient managementClient) {
        this.client = managementClient;
    }

    public List<Map<String, Object>> listExchanges() {
        List<Map<String, Object>> all = client.get()
                .uri("/exchanges/{vhost}", VHOST)
                .retrieve().body(LIST_OF_MAPS);
        return all.stream()
                .filter(e -> {
                    String n = String.valueOf(e.get("name"));
                    return !n.isEmpty() && !n.startsWith("amq.");
                })
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.get("name"));
                    m.put("type", e.get("type"));
                    m.put("durable", e.get("durable"));
                    return m;
                })
                .toList();
    }

    public List<Map<String, Object>> listQueues() {
        List<Map<String, Object>> all = client.get()
                .uri("/queues/{vhost}", VHOST)
                .retrieve().body(LIST_OF_MAPS);
        return all.stream()
                .map(q -> {
                    Map<String, Object> args = q.get("arguments") instanceof Map<?, ?> a
                            ? castMap(a) : Map.of();
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", q.get("name"));
                    m.put("messages", q.getOrDefault("messages", 0));
                    m.put("consumers", q.getOrDefault("consumers", 0));
                    m.put("durable", q.get("durable"));
                    m.put("deadLetterExchange", args.getOrDefault("x-dead-letter-exchange", ""));
                    return m;
                })
                .toList();
    }

    public List<Map<String, Object>> listBindings() {
        List<Map<String, Object>> all = client.get()
                .uri("/bindings/{vhost}", VHOST)
                .retrieve().body(LIST_OF_MAPS);
        return all.stream()
                .filter(b -> {
                    String s = String.valueOf(b.get("source"));
                    return !s.isEmpty() && !s.startsWith("amq.");
                })
                .map(b -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("source", b.get("source"));
                    m.put("destination", b.get("destination"));
                    m.put("destinationType", b.get("destination_type"));
                    m.put("routingKey", b.get("routing_key"));
                    return m;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }
}