package com.mantenimiento.telemetria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.telemetria.dto.LecturaEvent;
import com.mantenimiento.telemetria.messaging.LecturaProducer;
import com.mantenimiento.telemetria.model.Lectura;
import com.mantenimiento.telemetria.repository.LecturaRepository;

@RestController
@RequestMapping("/api/lecturas")
public class LecturaController {

    private final LecturaProducer producer;
    private final LecturaRepository repository;

    public LecturaController(LecturaProducer producer, LecturaRepository repository) {
        this.producer = producer;
        this.repository = repository;
    }

    
    @PostMapping
    public ResponseEntity<Void> publicar(@RequestBody LecturaEvent e) {
        producer.publicar(e);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping
    public List<Lectura> consultar(@RequestParam String equipoId) {
        return repository.findTop100ByEquipoIdOrderByTimestampDesc(equipoId);
    }
}