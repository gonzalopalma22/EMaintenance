package com.mantenimiento.analitica.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mantenimiento.analitica.dto.Resumen;

class AgregadorServiceTest {

    private static final long VENTANA_SEGUNDOS = 300;

    @Test
    void calculaPromedioMinimoMaximoYUltimo() {
        AgregadorService agregador = new AgregadorService(VENTANA_SEGUNDOS);
        long ahora = System.currentTimeMillis();

        agregador.registrar("camion-01", "temperatura", 70.0, ahora);
        agregador.registrar("camion-01", "temperatura", 80.0, ahora);
        agregador.registrar("camion-01", "temperatura", 90.0, ahora);

        List<Resumen> resumenes = agregador.resumenes("camion-01");

        assertEquals(1, resumenes.size());
        Resumen r = resumenes.get(0);
        assertEquals(3, r.muestras());
        assertEquals(80.0, r.promedio(), 0.001);
        assertEquals(70.0, r.minimo(), 0.001);
        assertEquals(90.0, r.maximo(), 0.001);
        assertEquals(90.0, r.ultimo(), 0.001);
    }

    @Test
    void descartaLasLecturasFueraDeLaVentana() {
        AgregadorService agregador = new AgregadorService(VENTANA_SEGUNDOS);
        long haceDiezMinutos = System.currentTimeMillis() - 10 * 60 * 1000;

        agregador.registrar("camion-01", "presion", 5.0, haceDiezMinutos);

        assertTrue(agregador.resumenes(null).isEmpty());
    }

    @Test
    void separaPorEquipoYPorSensor() {
        AgregadorService agregador = new AgregadorService(VENTANA_SEGUNDOS);
        long ahora = System.currentTimeMillis();

        agregador.registrar("camion-01", "temperatura", 70.0, ahora);
        agregador.registrar("camion-01", "vibracion", 2.0, ahora);
        agregador.registrar("camion-02", "temperatura", 60.0, ahora);

        assertEquals(3, agregador.resumenes(null).size());
        assertEquals(2, agregador.resumenes("camion-01").size());
        assertEquals(1, agregador.resumenes("camion-02").size());
    }

    @Test
    void llevaLosContadoresDeEstado() {
        AgregadorService agregador = new AgregadorService(VENTANA_SEGUNDOS);

        agregador.registrar("camion-01", "temperatura", 70.0, System.currentTimeMillis());
        agregador.registrarDescartada();
        agregador.registrarPublicado();
        agregador.registrarPublicado();

        assertEquals(1L, agregador.estado().get("lecturasConsumidas"));
        assertEquals(1L, agregador.estado().get("lecturasDescartadas"));
        assertEquals(2L, agregador.estado().get("resumenesPublicados"));
    }
}