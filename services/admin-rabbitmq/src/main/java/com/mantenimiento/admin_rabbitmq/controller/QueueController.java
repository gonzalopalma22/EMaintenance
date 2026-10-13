package com.mantenimiento.admin_rabbitmq.controller;

import com.mantenimiento.admin_rabbitmq.dto.QueueRequest;
import com.mantenimiento.admin_rabbitmq.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queues")
public class QueueController {

    private final RabbitAdminService service;

    public QueueController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody QueueRequest req) {
        service.createQueue(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name,
                                       @RequestParam(defaultValue = "false") boolean force) {
        service.deleteQueue(name, force);
        return ResponseEntity.noContent().build();
    }
}