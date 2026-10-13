package com.mantenimiento.admin_rabbitmq.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.admin_rabbitmq.dto.BindingRequest;
import com.mantenimiento.admin_rabbitmq.service.RabbitAdminService;
import com.mantenimiento.admin_rabbitmq.service.RabbitQueryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bindings")
public class BindingController {

    private final RabbitAdminService service;
    private final RabbitQueryService queryService;

    public BindingController(RabbitAdminService service, RabbitQueryService queryService) {
        this.service = service;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody BindingRequest req) {
        service.createBinding(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@Valid @RequestBody BindingRequest req) {
        service.deleteBinding(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return queryService.listBindings();
    }
}