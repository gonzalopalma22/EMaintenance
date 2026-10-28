package com.mantenimiento.ordenes_trabajo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.ordenes_trabajo.dto.TecnicoDto;
import com.mantenimiento.ordenes_trabajo.service.TecnicoService;

@RestController
@RequestMapping("/api/ordenes/tecnicos")
public class TecnicoController {

    private final TecnicoService service;

    public TecnicoController(TecnicoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TecnicoDto> listar() {
        return service.listar();
    }
}