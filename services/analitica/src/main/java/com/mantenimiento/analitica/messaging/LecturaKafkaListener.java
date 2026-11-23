package com.mantenimiento.analitica.messaging;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.mantenimiento.analitica.service.AgregadorService;

@Component
public class LecturaKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(LecturaKafkaListener.class);

    private final AgregadorService agregador;
    private final JsonParser parser = JsonParserFactory.getJsonParser();

    public LecturaKafkaListener(AgregadorService agregador) {
        this.agregador = agregador;
    }

    @KafkaListener(topics = "${analitica.topic-lecturas}")
    public void consumir(ConsumerRecord<String, String> registro) {
        Map<String, Object> lectura;
        try {
            lectura = parser.parseMap(registro.value());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("El mensaje no es un JSON válido", e);
        }

        String equipoId = texto(lectura, "equipoId");
        String tipo = texto(lectura, "tipo");
        if (!(lectura.get("valor") instanceof Number numero)) {
            throw new IllegalArgumentException("Falta el campo numérico 'valor'");
        }
        double valor = numero.doubleValue();
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException("El campo 'valor' no es un número finito");
        }

        agregador.registrar(equipoId, tipo, valor, registro.timestamp());
        log.debug("Lectura consumida. equipo={} tipo={} valor={} particion={} offset={}",
                equipoId, tipo, valor, registro.partition(), registro.offset());
    }

    private String texto(Map<String, Object> lectura, String campo) {
        if (lectura.get(campo) instanceof String s && !s.isBlank()) {
            return s;
        }
        throw new IllegalArgumentException("Falta el campo '" + campo + "'");
    }
}