package com.mantenimiento.ordenes_trabajo.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.ordenes_trabajo.dto.CambioEstadoRequest;
import com.mantenimiento.ordenes_trabajo.exception.ConflictException;
import com.mantenimiento.ordenes_trabajo.exception.NotFoundException;
import com.mantenimiento.ordenes_trabajo.model.OrdenTrabajo;
import com.mantenimiento.ordenes_trabajo.repository.OrdenTrabajoRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenTrabajoController {

    // estado actual -> estados permitidos
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            "ABIERTA", Set.of("ASIGNADA", "CERRADA"),
            "ASIGNADA", Set.of("EN_CURSO", "CERRADA"),
            "EN_CURSO", Set.of("CERRADA"),
            "CERRADA", Set.of());

    private final OrdenTrabajoRepository repository;

    public OrdenTrabajoController(OrdenTrabajoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<OrdenTrabajo> listar(@RequestParam(required = false) String estado) {
        return estado == null
                ? repository.findTop100ByOrderByCreadaEnDesc()
                : repository.findTop100ByEstadoOrderByCreadaEnDesc(estado);
    }

    @GetMapping("/{id}")
    public OrdenTrabajo obtener(@PathVariable UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden no existe: " + id));
    }

    @PatchMapping("/{id}/estado")
    @Transactional
    public OrdenTrabajo cambiarEstado(@PathVariable UUID id,
                                      @Valid @RequestBody CambioEstadoRequest req) {
        OrdenTrabajo o = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden no existe: " + id));
        if (!TRANSICIONES.get(o.getEstado()).contains(req.estado())) {
            throw new ConflictException(
                    "Transición no permitida: " + o.getEstado() + " -> " + req.estado());
        }
        o.cambiarEstado(req.estado(), req.asignadoA());
        return o;
    }
}