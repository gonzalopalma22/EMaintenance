package com.mantenimiento.simulador_sensores.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mantenimiento.simulador_sensores.dto.LecturaEvent;

@Service
public class SimuladorService {

    private static final Logger log = LoggerFactory.getLogger(SimuladorService.class);
    private static final String EXCHANGE = "mantenimiento.lecturas.exchange";
    private static final String RK = "lectura.recibida";

    // Valores base (equipo sano) y valores máximos (falla total) de cada sensor
    private static final double TEMP_NORMAL = 80.0, TEMP_FALLA = 160.0;   // °C
    private static final double VIB_NORMAL = 3.0, VIB_FALLA = 14.0;       // mm/s
    private static final double PRES_NORMAL = 6.0, PRES_FALLA = 12.0;     // bar

    private final RabbitTemplate rabbitTemplate;
    private final List<String> equipos;
    private final int porcentajeInvalidas;

    // Nivel de degradación por equipo: 0.0 = sano, 1.0 = falla total.
    // Los 3 sensores se calculan a partir de este mismo valor, por eso suben juntos.
    private final Map<String, Double> degradacion = new ConcurrentHashMap<>();
    private final Map<String, Boolean> enFalla = new ConcurrentHashMap<>();

    public SimuladorService(RabbitTemplate rabbitTemplate,
                            @Value("${simulador.equipos}") String equipos,
                            @Value("${simulador.porcentaje-invalidas}") int porcentajeInvalidas) {
        this.rabbitTemplate = rabbitTemplate;
        this.equipos = Arrays.stream(equipos.split(",")).map(String::trim).toList();
        this.porcentajeInvalidas = porcentajeInvalidas;
        this.equipos.forEach(e -> degradacion.put(e, 0.0));
    }

    @Scheduled(fixedDelayString = "${simulador.intervalo-ms}")
    public void generar() {
        var rnd = ThreadLocalRandom.current();
        for (String equipo : equipos) {
            double d = degradacion.get(equipo);
            if (Boolean.TRUE.equals(enFalla.get(equipo))) {
                d = Math.min(1.0, d + 0.04 + rnd.nextDouble() * 0.02);  // ~20 ciclos hasta la falla total
                degradacion.put(equipo, d);
            }

            double temp = TEMP_NORMAL + d * (TEMP_FALLA - TEMP_NORMAL) + ruido(rnd.nextDouble(), 3.0);
            double vib = VIB_NORMAL + d * (VIB_FALLA - VIB_NORMAL) + ruido(rnd.nextDouble(), 0.5);
            double pres = PRES_NORMAL + d * (PRES_FALLA - PRES_NORMAL) + ruido(rnd.nextDouble(), 0.3);

            publicar(equipo, "temp-motor", "temperatura", temp);
            publicar(equipo, "vib-motor", "vibracion", vib);
            publicar(equipo, "pres-hidraulica", "presion", pres);
        }
    }

    /** Ruido simétrico entre -amplitud y +amplitud. */
    private double ruido(double r, double amplitud) {
        return (r * 2 - 1) * amplitud;
    }

    private void publicar(String equipo, String sensorId, String tipo, double valor) {
        double v = Math.max(0.0, valor);
        if (ThreadLocalRandom.current().nextInt(100) < porcentajeInvalidas) {
            v = 99999.0;   // lectura inválida a propósito, para probar la DLQ
        }
        LecturaEvent e = new LecturaEvent(UUID.randomUUID(), 1, equipo,
                sensorId, tipo, Math.round(v * 100) / 100.0, Instant.now());
        rabbitTemplate.convertAndSend(EXCHANGE, RK, e);
        log.info("Lectura enviada. equipo={} tipo={} valor={}", equipo, tipo, e.valor());
    }

    public void iniciarFalla(String equipo) {
        validar(equipo);
        enFalla.put(equipo, true);
        log.warn("FALLA inyectada. equipo={}", equipo);
    }

    public void volverNormal(String equipo) {
        validar(equipo);
        enFalla.put(equipo, false);
        degradacion.put(equipo, 0.0);
        log.info("Equipo normalizado. equipo={}", equipo);
    }

    /** Devuelve siempre todos los equipos, con true si están en falla y false si están normales. */
    public Map<String, Boolean> estado() {
        Map<String, Boolean> resultado = new LinkedHashMap<>();
        for (String equipo : equipos) {
            resultado.put(equipo, Boolean.TRUE.equals(enFalla.get(equipo)));
        }
        return resultado;
    }

    private void validar(String equipo) {
        if (!degradacion.containsKey(equipo)) {
            throw new IllegalArgumentException("Equipo desconocido: " + equipo);
        }
    }
}