package com.mantenimiento.analitica.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mantenimiento.analitica.dto.Resumen;

/** Mantiene en memoria las lecturas recientes de cada equipo y sensor y calcula estadísticas por ventana. */
@Service
public class AgregadorService {

    private record Muestra(long instanteMs, double valor) {
    }

    private final Map<String, Deque<Muestra>> muestras = new ConcurrentHashMap<>();
    private final long ventanaSegundos;
    private final AtomicLong consumidas = new AtomicLong();
    private final AtomicLong descartadas = new AtomicLong();
    private final AtomicLong publicados = new AtomicLong();

    public AgregadorService(@Value("${analitica.ventana-segundos}") long ventanaSegundos) {
        this.ventanaSegundos = ventanaSegundos;
    }

    public void registrar(String equipoId, String tipo, double valor, long instanteMs) {
        Deque<Muestra> cola = muestras.computeIfAbsent(equipoId + "|" + tipo, k -> new ConcurrentLinkedDeque<>());
        cola.addLast(new Muestra(instanteMs, valor));
        podar(cola, System.currentTimeMillis());
        consumidas.incrementAndGet();
    }

    public List<Resumen> resumenes(String equipoId) {
        long ahora = System.currentTimeMillis();
        List<Resumen> salida = new ArrayList<>();

        for (Map.Entry<String, Deque<Muestra>> entrada : muestras.entrySet()) {
            String[] partes = entrada.getKey().split("\\|", 2);
            if (equipoId != null && !partes[0].equals(equipoId)) {
                continue;
            }
            podar(entrada.getValue(), ahora);
            List<Muestra> copia = new ArrayList<>(entrada.getValue());
            if (copia.isEmpty()) {
                continue;
            }
            DoubleSummaryStatistics est = copia.stream().mapToDouble(Muestra::valor).summaryStatistics();
            salida.add(new Resumen(
                    partes[0], partes[1], ventanaSegundos, est.getCount(),
                    redondear(est.getAverage()), redondear(est.getMin()), redondear(est.getMax()),
                    redondear(copia.get(copia.size() - 1).valor()), Instant.ofEpochMilli(ahora)));
        }

        salida.sort(Comparator.comparing(Resumen::equipoId).thenComparing(Resumen::tipo));
        return salida;
    }

    public void registrarDescartada() {
        descartadas.incrementAndGet();
    }

    public void registrarPublicado() {
        publicados.incrementAndGet();
    }

    public Map<String, Object> estado() {
        return Map.of(
                "lecturasConsumidas", consumidas.get(),
                "lecturasDescartadas", descartadas.get(),
                "resumenesPublicados", publicados.get(),
                "ventanaSegundos", ventanaSegundos);
    }

    private void podar(Deque<Muestra> cola, long ahoraMs) {
        long limite = ventanaSegundos * 1000;
        Muestra primera;
        while ((primera = cola.peekFirst()) != null && ahoraMs - primera.instanteMs() > limite) {
            cola.pollFirst();
        }
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}