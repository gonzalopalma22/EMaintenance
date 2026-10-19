package com.mantenimiento.ordenes_trabajo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mantenimiento.ordenes_trabajo.model.OrdenTrabajo;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, UUID> {

    boolean existsByAlertaId(UUID alertaId);

    List<OrdenTrabajo> findTop100ByOrderByCreadaEnDesc();

    List<OrdenTrabajo> findTop100ByEstadoOrderByCreadaEnDesc(String estado);
}