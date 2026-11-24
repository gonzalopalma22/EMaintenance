package com.mantenimiento.admin_kafka.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class TopicRequestValidationTest {

    private static ValidatorFactory fabrica;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private Set<ConstraintViolation<TopicRequest>> validar(TopicRequest req) {
        return fabrica.getValidator().validate(req);
    }

    @Test
    void aceptaUnaSolicitudValida() {
        assertTrue(validar(new TopicRequest("telemetria.lecturas", 3, 3, null)).isEmpty());
    }

    @Test
    void rechazaNombresConEspaciosOCaracteresRaros() {
        assertEquals(1, validar(new TopicRequest("mi topico", 3, 3, null)).size());
        assertEquals(1, validar(new TopicRequest("topico/raro", 3, 3, null)).size());
    }

    @Test
    void rechazaNombreVacio() {
        assertTrue(validar(new TopicRequest("", 3, 3, null)).size() >= 1);
    }

    @Test
    void rechazaParticionesFueraDeRango() {
        assertEquals(1, validar(new TopicRequest("demo", 0, 3, null)).size());
        assertEquals(1, validar(new TopicRequest("demo", 51, 3, null)).size());
    }

    @Test
    void rechazaFactorDeReplicacionNuloOFueraDeRango() {
        assertEquals(1, validar(new TopicRequest("demo", 3, null, null)).size());
        assertEquals(1, validar(new TopicRequest("demo", 3, 11, null)).size());
    }
}