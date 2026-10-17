package com.mantenimiento.telemetria.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "lecturas", indexes = {
        @Index(name = "idx_lectura_equipo_ts", columnList = "equipoId, timestamp")
})
public class Lectura {

    @Id
    private UUID eventId;               

    @Column(nullable = false)
    private String equipoId;

    @Column(nullable = false)
    private String sensorId;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private double valor;

    @Column(nullable = false)
    private Instant timestamp;

    protected Lectura() {}

    public Lectura(UUID eventId, String equipoId, String sensorId,
                   String tipo, double valor, Instant timestamp) {
        this.eventId = eventId;
        this.equipoId = equipoId;
        this.sensorId = sensorId;
        this.tipo = tipo;
        this.valor = valor;
        this.timestamp = timestamp;
    }

    public UUID getEventId() { return eventId; }
    public String getEquipoId() { return equipoId; }
    public String getSensorId() { return sensorId; }
    public String getTipo() { return tipo; }
    public double getValor() { return valor; }
    public Instant getTimestamp() { return timestamp; }
}