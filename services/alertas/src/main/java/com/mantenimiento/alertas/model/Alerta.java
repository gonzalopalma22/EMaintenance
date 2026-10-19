package com.mantenimiento.alertas.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID lecturaEventId;    
    @Column(nullable = false)
    private String equipoId;

    @Column(nullable = false)
    private String sensorId;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private double valor;

    @Column(nullable = false)
    private double umbral;

    @Column(nullable = false)
    private String severidad;

    @Column(nullable = false)
    private String estado;           

    @Column(nullable = false)
    private Instant generadaEn;

    protected Alerta() {}

    public Alerta(UUID id, UUID lecturaEventId, String equipoId, String sensorId,
                  String tipo, double valor, double umbral, String severidad,
                  Instant generadaEn) {
        this.id = id;
        this.lecturaEventId = lecturaEventId;
        this.equipoId = equipoId;
        this.sensorId = sensorId;
        this.tipo = tipo;
        this.valor = valor;
        this.umbral = umbral;
        this.severidad = severidad;
        this.estado = "ABIERTA";
        this.generadaEn = generadaEn;
    }

    public void reconocer() { this.estado = "RECONOCIDA"; }

    public UUID getId() { return id; }
    public UUID getLecturaEventId() { return lecturaEventId; }
    public String getEquipoId() { return equipoId; }
    public String getSensorId() { return sensorId; }
    public String getTipo() { return tipo; }
    public double getValor() { return valor; }
    public double getUmbral() { return umbral; }
    public String getSeveridad() { return severidad; }
    public String getEstado() { return estado; }
    public Instant getGeneradaEn() { return generadaEn; }
}