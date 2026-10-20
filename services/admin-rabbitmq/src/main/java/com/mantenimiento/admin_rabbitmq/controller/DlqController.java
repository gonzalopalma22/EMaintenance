package com.mantenimiento.admin_rabbitmq.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.admin_rabbitmq.service.DlqService;

@RestController
@RequestMapping("/api/dlq")
public class DlqController {

    private final DlqService service;

    public DlqController(DlqService service) {
        this.service = service;
    }

    @GetMapping("/{cola}/mensajes")
    public List<Map<String, Object>> ver(@PathVariable String cola,
                                         @RequestParam(defaultValue = "10") int limite) {
        return service.ver(cola, limite);
    }

    @PostMapping("/{cola}/reprocesar")
    public Map<String, Object> reprocesar(@PathVariable String cola,
                                          @RequestParam(defaultValue = "10") int cantidad) {
        return Map.of("cola", cola, "reprocesados", service.reprocesar(cola, cantidad));
    }
}