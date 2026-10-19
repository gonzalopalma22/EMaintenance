package com.mantenimiento.ordenes_trabajo.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ordenes_trabajo")
public class OrdenTrabajo {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID alertaId;          

    @Column(nullable = false)
    private String equipoId;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private String prioridad;       

    @Column(nullable = false)
    private String estado;          

    private String asignadoA;

    @Column(nullable = false)
    private Instant creadaEn;

    private Instant cerradaEn;

    protected OrdenTrabajo() {}

    public OrdenTrabajo(UUID id, UUID alertaId, String equipoId,
                        String descripcion, String prioridad, Instant creadaEn) {
        this.id = id;
        this.alertaId = alertaId;
        this.equipoId = equipoId;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.estado = "ABIERTA";
        this.creadaEn = creadaEn;
    }

    public void cambiarEstado(String nuevo, String asignadoA) {
        this.estado = nuevo;
        if (asignadoA != null) this.asignadoA = asignadoA;
        if ("CERRADA".equals(nuevo)) this.cerradaEn = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAlertaId() { return alertaId; }
    public String getEquipoId() { return equipoId; }
    public String getDescripcion() { return descripcion; }
    public String getPrioridad() { return prioridad; }
    public String getEstado() { return estado; }
    public String getAsignadoA() { return asignadoA; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getCerradaEn() { return cerradaEn; }
}