package com.mantenimiento.admin_rabbitmq.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.admin_rabbitmq.dto.ExchangeRequest;
import com.mantenimiento.admin_rabbitmq.service.RabbitAdminService;
import com.mantenimiento.admin_rabbitmq.service.RabbitQueryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exchanges")
public class ExchangeController {

    private final RabbitAdminService service;
    private final RabbitQueryService queryService;

    public ExchangeController(RabbitAdminService service, RabbitQueryService queryService) {
        this.service = service;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ExchangeRequest req) {
        service.createExchange(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        service.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return queryService.listExchanges();
    }
}