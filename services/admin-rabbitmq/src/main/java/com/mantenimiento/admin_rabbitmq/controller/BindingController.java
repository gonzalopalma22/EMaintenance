package com.mantenimiento.admin_rabbitmq.controller;

import com.mantenimiento.admin_rabbitmq.dto.BindingRequest;
import com.mantenimiento.admin_rabbitmq.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bindings")
public class BindingController {

    private final RabbitAdminService service;

    public BindingController(RabbitAdminService service) {
        this.service = service;
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
}