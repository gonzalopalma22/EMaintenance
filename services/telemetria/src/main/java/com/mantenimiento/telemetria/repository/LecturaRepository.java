package com.mantenimiento.telemetria.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mantenimiento.telemetria.model.Lectura;

public interface LecturaRepository extends JpaRepository<Lectura, UUID> {

    List<Lectura> findTop100ByEquipoIdOrderByTimestampDesc(String equipoId);
}