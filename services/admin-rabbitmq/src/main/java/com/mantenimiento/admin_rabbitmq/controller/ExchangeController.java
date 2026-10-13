package com.mantenimiento.admin_rabbitmq.controller;



import com.mantenimiento.admin_rabbitmq.dto.ExchangeRequest;
import com.mantenimiento.admin_rabbitmq.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchanges")
public class ExchangeController {

    private final RabbitAdminService service;

    public ExchangeController(RabbitAdminService service) {
        this.service = service;
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
} 
