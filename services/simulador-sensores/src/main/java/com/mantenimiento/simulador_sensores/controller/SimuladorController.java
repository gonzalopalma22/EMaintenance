package com.mantenimiento.simulador_sensores.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.simulador_sensores.service.SimuladorService;

@RestController
@RequestMapping("/api/simulador")
public class SimuladorController {

    private final SimuladorService service;

    public SimuladorController(SimuladorService service) {
        this.service = service;
    }

    @PostMapping("/falla/{equipoId}")
    public Map<String, String> falla(@PathVariable String equipoId) {
        service.iniciarFalla(equipoId);
        return Map.of("equipo", equipoId, "modo", "FALLA");
    }

    @PostMapping("/normal/{equipoId}")
    public Map<String, String> normal(@PathVariable String equipoId) {
        service.volverNormal(equipoId);
        return Map.of("equipo", equipoId, "modo", "NORMAL");
    }

    @GetMapping("/estado")
    public Map<String, Boolean> estado() {
        return service.estado();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail desconocido(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}