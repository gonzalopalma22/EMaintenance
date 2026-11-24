package com.mantenimiento.analitica.messaging;

import java.util.Map;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;

import com.mantenimiento.analitica.dto.LecturaEvent;

/**
 * Convierte el JSON recibido de Kafka en un {@link LecturaEvent} validado.
 * Cualquier payload inválido lanza {@link IllegalArgumentException}, que el
 * manejador de errores envía directo a la DLT sin reintentos.
 */
public final class LecturaEventMapper {

    private static final JsonParser PARSER = JsonParserFactory.getJsonParser();

    private LecturaEventMapper() {
    }

    public static LecturaEvent desdeJson(String json) {
        Map<String, Object> campos;
        try {
            campos = PARSER.parseMap(json);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("El mensaje no es un JSON válido", e);
        }

        String equipoId = textoObligatorio(campos, "equipoId");
        String tipo = textoObligatorio(campos, "tipo");

        if (!(campos.get("valor") instanceof Number numero)) {
            throw new IllegalArgumentException("Falta el campo numérico 'valor'");
        }
        double valor = numero.doubleValue();
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException("El campo 'valor' no es un número finito");
        }

        return new LecturaEvent(
                textoOpcional(campos, "eventId"),
                equipoId,
                textoOpcional(campos, "sensorId"),
                tipo,
                valor,
                textoOpcional(campos, "timestamp"));
    }

    private static String textoObligatorio(Map<String, Object> campos, String campo) {
        if (campos.get(campo) instanceof String s && !s.isBlank()) {
            return s;
        }
        throw new IllegalArgumentException("Falta el campo '" + campo + "'");
    }

    private static String textoOpcional(Map<String, Object> campos, String campo) {
        return campos.get(campo) instanceof String s ? s : null;
    }
}