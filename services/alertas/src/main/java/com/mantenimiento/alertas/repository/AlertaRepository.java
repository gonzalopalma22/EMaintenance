package com.mantenimiento.alertas.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mantenimiento.alertas.model.Alerta;

public interface AlertaRepository extends JpaRepository<Alerta, UUID> {

    boolean existsByLecturaEventIdAndTipo(UUID lecturaEventId, String tipo);

    List<Alerta> findTop100ByEstadoOrderByGeneradaEnDesc(String estado);

    List<Alerta> findTop100ByOrderByGeneradaEnDesc();

    boolean existsByEquipoIdAndTipoAndEstado(String equipoId, String tipo, String estado);
}