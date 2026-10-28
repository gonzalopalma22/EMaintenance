package com.mantenimiento.alertas.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.alertas.dto.AlertaEvent;
import com.mantenimiento.alertas.exception.NotFoundException;
import com.mantenimiento.alertas.model.Alerta;
import com.mantenimiento.alertas.repository.AlertaRepository;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private static final Logger log = LoggerFactory.getLogger(AlertaController.class);
    private static final String EXCHANGE = "mantenimiento.eventos.exchange";
    private static final String RK = "alerta.reconocida";

    private final AlertaRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public AlertaController(AlertaRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
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

        // Si ya estaba reconocida no se vuelve a publicar: evita órdenes duplicadas
        if ("RECONOCIDA".equals(a.getEstado())) {
            return a;
        }
        a.reconocer();

        AlertaEvent evento = new AlertaEvent(a.getId(), a.getLecturaEventId(), a.getEquipoId(),
                a.getSensorId(), a.getTipo(), a.getValor(), a.getUmbral(),
                a.getSeveridad(), a.getGeneradaEn());

        // Se publica solo si la transacción se confirma
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(EXCHANGE, RK, evento);
                log.info("Alerta reconocida -> evento publicado. alertaId={} equipo={} tipo={}",
                        evento.alertaId(), evento.equipoId(), evento.tipo());
            }
        });
        return a;
    }
}