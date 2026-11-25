package com.mantenimiento.analitica.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.kafka.common.serialization.Serializer;

import com.mantenimiento.analitica.dto.Resumen;

/** Serializa un {@link Resumen} a JSON (UTF-8) para publicarlo en analitica.resumen. */
public class ResumenSerializer implements Serializer<Resumen> {

    /** Versión del contrato del evento; se incrementa si cambian sus campos. */
    static final int SCHEMA_VERSION = 1;

    @Override
    public byte[] serialize(String topic, Resumen r) {
        if (r == null) {
            return null;
        }
        String json = String.format(Locale.ROOT,
                "{\"schemaVersion\":%d,\"equipoId\":%s,\"tipo\":%s,\"ventanaSegundos\":%d,"
                        + "\"muestras\":%d,\"promedio\":%s,\"minimo\":%s,\"maximo\":%s,\"ultimo\":%s,"
                        + "\"calculadoEn\":%s}",
                SCHEMA_VERSION, texto(r.equipoId()), texto(r.tipo()), r.ventanaSegundos(),
                r.muestras(), numero(r.promedio()), numero(r.minimo()), numero(r.maximo()),
                numero(r.ultimo()),
                texto(r.calculadoEn() == null ? null : r.calculadoEn().toString()));
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static String texto(String valor) {
        if (valor == null) {
            return "null";
        }
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Un NaN o infinito no es JSON válido; se publica como null. */
    private static String numero(double valor) {
        return Double.isFinite(valor) ? Double.toString(valor) : "null";
    }
}