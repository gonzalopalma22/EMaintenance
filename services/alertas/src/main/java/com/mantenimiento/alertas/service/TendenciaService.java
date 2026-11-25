package com.mantenimiento.alertas.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.mantenimiento.alertas.dto.ResumenEvent;

/**
 * Alerta de tendencia: compara el promedio de la ventana de cada equipo y sensor con los umbrales
 * de alertas.reglas.* y avisa cuando el nivel cambia, así no se repite el aviso cada 30 segundos.
 * Complementa la alerta por lectura puntual: detecta un deterioro sostenido antes de que una sola lectura lo supere.
 */
@Service
public class TendenciaService {

    public enum Nivel { NORMAL, EN_RIESGO, CRITICO }

    private static final Logger log = LoggerFactory.getLogger(TendenciaService.class);

    private final Environment entorno;
    private final Map<String, Nivel> niveles = new ConcurrentHashMap<>();
    private final AtomicLong evaluados = new AtomicLong();
    private final AtomicLong descartados = new AtomicLong();

    public TendenciaService(Environment entorno) {
        this.entorno = entorno;
    }

    public void evaluar(ResumenEvent resumen) {
        Nivel nuevo = clasificar(resumen.tipo(), resumen.promedio());
        Nivel anterior = niveles.put(resumen.equipoId() + "|" + resumen.tipo(), nuevo);
        evaluados.incrementAndGet();

        if (nuevo != Nivel.NORMAL && nuevo != anterior) {
            log.warn("Tendencia {}. equipo={} tipo={} promedio={} ventana={}s muestras={}",
                    nuevo, resumen.equipoId(), resumen.tipo(), resumen.promedio(),
                    resumen.ventanaSegundos(), resumen.muestras());
        } else if (nuevo == Nivel.NORMAL && anterior != null && anterior != Nivel.NORMAL) {
            log.info("Tendencia normalizada. equipo={} tipo={} promedio={}",
                    resumen.equipoId(), resumen.tipo(), resumen.promedio());
        }
    }

    Nivel clasificar(String tipo, Double promedio) {
        Double umbral = entorno.getProperty("alertas.reglas." + tipo + ".umbral", Double.class);
        Double critico = entorno.getProperty("alertas.reglas." + tipo + ".critico", Double.class);
        if (promedio == null || umbral == null) {
            return Nivel.NORMAL;
        }
        if (critico != null && promedio >= critico) {
            return Nivel.CRITICO;
        }
        return promedio >= umbral ? Nivel.EN_RIESGO : Nivel.NORMAL;
    }

    public void registrarDescartado() {
        descartados.incrementAndGet();
    }

    public long evaluados() {
        return evaluados.get();
    }

    public long descartados() {
        return descartados.get();
    }
}