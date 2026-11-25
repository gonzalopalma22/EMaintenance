package com.mantenimiento.alertas.messaging;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;

import com.mantenimiento.alertas.dto.ResumenEvent;

/**
 * Convierte el JSON recibido de Kafka en un {@link ResumenEvent} validado.
 * Cualquier payload inválido lanza {@link IllegalArgumentException}, que el
 * manejador de errores envía directo a la DLT sin reintentos.
 */
public final class ResumenEventMapper {

    private static final int VERSION_SOPORTADA = 1;
    private static final JsonParser PARSER = JsonParserFactory.getJsonParser();

    private ResumenEventMapper() {
    }

    public static ResumenEvent desdeJson(String json) {
        Map<String, Object> campos;
        try {
            campos = PARSER.parseMap(json);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("El mensaje no es un JSON válido", e);
        }

        Integer version = entero(campos, "schemaVersion");
        if (version != null && version != VERSION_SOPORTADA) {
            throw new IllegalArgumentException("Versión de esquema no soportada: " + version);
        }

        String equipoId = textoObligatorio(campos, "equipoId");
        String tipo = textoObligatorio(campos, "tipo");
        Double promedio = numero(campos, "promedio");
        if (promedio == null) {
            throw new IllegalArgumentException("Falta el campo numérico 'promedio'");
        }

        return new ResumenEvent(
                version == null ? VERSION_SOPORTADA : version,
                equipoId,
                tipo,
                largo(campos, "ventanaSegundos"),
                largo(campos, "muestras"),
                promedio,
                numero(campos, "minimo"),
                numero(campos, "maximo"),
                numero(campos, "ultimo"),
                instante(campos, "calculadoEn"));
    }

    private static String textoObligatorio(Map<String, Object> campos, String campo) {
        if (campos.get(campo) instanceof String s && !s.isBlank()) {
            return s;
        }
        throw new IllegalArgumentException("Falta el campo '" + campo + "'");
    }

    private static Double numero(Map<String, Object> campos, String campo) {
        if (campos.get(campo) instanceof Number n) {
            double valor = n.doubleValue();
            if (!Double.isFinite(valor)) {
                throw new IllegalArgumentException("El campo '" + campo + "' no es un número finito");
            }
            return valor;
        }
        return null;
    }

    private static Long largo(Map<String, Object> campos, String campo) {
        return campos.get(campo) instanceof Number n ? n.longValue() : null;
    }

    private static Integer entero(Map<String, Object> campos, String campo) {
        return campos.get(campo) instanceof Number n ? n.intValue() : null;
    }

    private static Instant instante(Map<String, Object> campos, String campo) {
        if (campos.get(campo) instanceof String s) {
            try {
                return Instant.parse(s);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("El campo '" + campo + "' no es una fecha ISO-8601", e);
            }
        }
        return null;
    }
}