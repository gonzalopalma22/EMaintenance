package com.mantenimiento.alertas.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.alertas.exception.NotFoundException;
import com.mantenimiento.alertas.model.Alerta;
import com.mantenimiento.alertas.repository.AlertaRepository;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaRepository repository;

    public AlertaController(AlertaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Alerta> listar(@RequestParam(required = false) String estado) {
        return estado == null
                ? repository.findTop100ByOrderByGeneradaEnDesc()
                : repository.findTop100ByEstadoOrderByGeneradaEnDesc(estado);
    }

    @PatchMapping("/{id}/reconocer")
    @Transactional
    public Alerta reconocer(@PathVariable UUID id) {
        Alerta a = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Alerta no existe: " + id));
        a.reconocer();
        return a;
    }
}