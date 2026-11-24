package com.mantenimiento.telemetria.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.kafka.common.serialization.Serializer;

import com.mantenimiento.telemetria.dto.LecturaEvent;

/** Serializa un {@link LecturaEvent} a JSON (UTF-8) para publicarlo en Kafka. */
public class LecturaEventSerializer implements Serializer<LecturaEvent> {

    @Override
    public byte[] serialize(String topic, LecturaEvent l) {
        if (l == null) {
            return null;
        }
        String json = String.format(Locale.ROOT,
                "{\"eventId\":\"%s\",\"schemaVersion\":%d,\"equipoId\":\"%s\",\"sensorId\":\"%s\","
                        + "\"tipo\":\"%s\",\"valor\":%s,\"timestamp\":\"%s\"}",
                l.eventId(), l.schemaVersion(), escapar(l.equipoId()), escapar(l.sensorId()),
                escapar(l.tipo()), l.valor(), l.timestamp());
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}