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

    // Tope de temperatura mientras un equipo está en falla (°C)
    private static final double TEMP_MAXIMA_FALLA = 150.0;

    private final RabbitTemplate rabbitTemplate;
    private final List<String> equipos;
    private final int porcentajeInvalidas;

    // temperatura actual por equipo; los equipos en falla suben progresivamente
    private final Map<String, Double> temperaturas = new ConcurrentHashMap<>();
    private final Map<String, Boolean> enFalla = new ConcurrentHashMap<>();

    public SimuladorService(RabbitTemplate rabbitTemplate,
                            @Value("${simulador.equipos}") String equipos,
                            @Value("${simulador.porcentaje-invalidas}") int porcentajeInvalidas) {
        this.rabbitTemplate = rabbitTemplate;
        this.equipos = Arrays.stream(equipos.split(",")).map(String::trim).toList();
        this.porcentajeInvalidas = porcentajeInvalidas;
        this.equipos.forEach(e -> temperaturas.put(e, 80.0));
    }

    @Scheduled(fixedDelayString = "${simulador.intervalo-ms}")
    public void generar() {
        var rnd = ThreadLocalRandom.current();
        for (String equipo : equipos) {
            double temp = temperaturas.get(equipo);
            if (Boolean.TRUE.equals(enFalla.get(equipo))) {
                temp += 3 + rnd.nextDouble() * 2;          // degradación progresiva
                temp = Math.min(temp, TEMP_MAXIMA_FALLA);  // tope
            } else {
                temp = 80 + rnd.nextDouble() * 6 - 3;      // normal: 77 a 83
            }
            temperaturas.put(equipo, temp);

            double valor = rnd.nextInt(100) < porcentajeInvalidas ? 99999.0 : temp;
            publicar(equipo, valor);
        }
    }

    private void publicar(String equipo, double valor) {
        LecturaEvent e = new LecturaEvent(UUID.randomUUID(), 1, equipo,
                "temp-motor", "temperatura", Math.round(valor * 10) / 10.0, Instant.now());
        rabbitTemplate.convertAndSend(EXCHANGE, RK, e);
        log.info("Lectura enviada. equipo={} valor={}", equipo, e.valor());
    }

    public void iniciarFalla(String equipo) {
        validar(equipo);
        enFalla.put(equipo, true);
        log.warn("FALLA inyectada. equipo={}", equipo);
    }

    public void volverNormal(String equipo) {
        validar(equipo);
        enFalla.put(equipo, false);
        temperaturas.put(equipo, 80.0);
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
        if (!temperaturas.containsKey(equipo)) {
            throw new IllegalArgumentException("Equipo desconocido: " + equipo);
        }
    }
}