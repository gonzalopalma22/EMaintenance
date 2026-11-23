package com.mantenimiento.analitica.service;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mantenimiento.analitica.dto.Resumen;

/** Publica periódicamente el resumen de cada equipo y sensor en el tópico analitica.resumen. */
@Service
public class PublicadorResumen {

    private static final Logger log = LoggerFactory.getLogger(PublicadorResumen.class);

    private final AgregadorService agregador;
    private final KafkaTemplate<String, String> template;
    private final String topicResumen;

    public PublicadorResumen(AgregadorService agregador,
                             KafkaTemplate<String, String> template,
                             @Value("${analitica.topic-resumen}") String topicResumen) {
        this.agregador = agregador;
        this.template = template;
        this.topicResumen = topicResumen;
    }

    @Scheduled(fixedDelayString = "${analitica.intervalo-resumen-ms}")
    public void publicar() {
        List<Resumen> resumenes = agregador.resumenes(null);
        for (Resumen r : resumenes) {
            // La clave es el equipo: todos sus resúmenes caen en la misma partición y conservan el orden
            template.send(topicResumen, r.equipoId(), aJson(r)).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("No se pudo publicar el resumen. equipo={} tipo={} motivo={}",
                            r.equipoId(), r.tipo(), error.getMessage());
                } else {
                    agregador.registrarPublicado();
                }
            });
        }
        if (!resumenes.isEmpty()) {
            log.info("Resúmenes publicados en {}: {}", topicResumen, resumenes.size());
        }
    }

    private static String aJson(Resumen r) {
        return String.format(Locale.ROOT,
                "{\"equipoId\":\"%s\",\"tipo\":\"%s\",\"ventanaSegundos\":%d,\"muestras\":%d,"
                        + "\"promedio\":%.2f,\"minimo\":%.2f,\"maximo\":%.2f,\"ultimo\":%.2f,\"calculadoEn\":\"%s\"}",
                escapar(r.equipoId()), escapar(r.tipo()), r.ventanaSegundos(), r.muestras(),
                r.promedio(), r.minimo(), r.maximo(), r.ultimo(), r.calculadoEn());
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}