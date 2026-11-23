package com.mantenimiento.analitica.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.analitica.dto.Resumen;
import com.mantenimiento.analitica.service.AgregadorService;

@RestController
@RequestMapping("/api/analitica")
public class AnaliticaController {

    private final AgregadorService agregador;

    public AnaliticaController(AgregadorService agregador) {
        this.agregador = agregador;
    }

    @GetMapping("/resumen")
    public List<Resumen> resumen(@RequestParam(required = false) String equipoId) {
        return agregador.resumenes(equipoId);
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return agregador.estado();
    }
}