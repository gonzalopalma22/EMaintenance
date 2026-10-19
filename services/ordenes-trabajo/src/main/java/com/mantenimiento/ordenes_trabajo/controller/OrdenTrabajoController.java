package com.mantenimiento.ordenes_trabajo.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
            "EN_CURSO", Set.of("REALIZADA"),
            "REALIZADA", Set.of("CERRADA", "EN_CURSO"),
            "CERRADA", Set.of());

    // Transiciones que solo puede hacer un Admin (encargado de la zona)
    private static final Set<String> SOLO_ADMIN = Set.of(
            "ABIERTA>ASIGNADA",
            "ABIERTA>CERRADA",
            "ASIGNADA>CERRADA",
            "REALIZADA>CERRADA",
            "REALIZADA>EN_CURSO");

    // Transiciones que solo puede hacer quien tiene la orden asignada (empleado)
    private static final Set<String> SOLO_ASIGNADO = Set.of(
            "ASIGNADA>EN_CURSO",
            "EN_CURSO>REALIZADA");

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
                                      @Valid @RequestBody CambioEstadoRequest req,
                                      @AuthenticationPrincipal Jwt jwt,
                                      Authentication auth) {
        OrdenTrabajo o = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden no existe: " + id));

        String actual = o.getEstado();
        String nuevo = req.estado();

        // 1. La transición debe existir (409)
        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevo)) {
            throw new ConflictException("Transición no permitida: " + actual + " -> " + nuevo);
        }

        // 2. Quién puede hacerla (403)
        String clave = actual + ">" + nuevo;
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_Admin".equals(a.getAuthority()));
        String usuario = jwt.getClaimAsString("preferred_username");

        if (SOLO_ADMIN.contains(clave)) {
            if (!esAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Solo un Admin puede hacer " + actual + " -> " + nuevo);
            }
        } else if (SOLO_ASIGNADO.contains(clave)) {
            boolean esElAsignado = usuario != null && o.getAsignadoA() != null
                    && o.getAsignadoA().trim().equalsIgnoreCase(usuario.trim());
            if (!esElAsignado) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Solo quien tiene asignada la orden puede hacer " + actual + " -> " + nuevo);
            }
        }

        // 3. Solo al asignar se acepta asignadoA; en el resto se ignora
        String asignadoA = null;
        if ("ASIGNADA".equals(nuevo)) {
            if (req.asignadoA() == null || req.asignadoA().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "asignadoA es obligatorio al asignar");
            }
            asignadoA = req.asignadoA().trim();
        }

        o.cambiarEstado(nuevo, asignadoA);
        return o;
    }
}