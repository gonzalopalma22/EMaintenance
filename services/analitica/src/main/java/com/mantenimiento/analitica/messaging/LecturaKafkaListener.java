package com.mantenimiento.analitica.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.mantenimiento.analitica.dto.LecturaEvent;
import com.mantenimiento.analitica.service.AgregadorService;

/** Capa de mensajería: recibe el evento, lo convierte a DTO y delega la lógica en el agregador. */
@Component
public class LecturaKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(LecturaKafkaListener.class);

    private final AgregadorService agregador;

    public LecturaKafkaListener(AgregadorService agregador) {
        this.agregador = agregador;
    }

    @KafkaListener(topics = "${analitica.topic-lecturas}")
    public void consumir(ConsumerRecord<String, String> registro) {
        LecturaEvent lectura = LecturaEventMapper.desdeJson(registro.value());

        agregador.registrar(lectura.equipoId(), lectura.tipo(), lectura.valor(), registro.timestamp());
        log.debug("Lectura consumida. equipo={} tipo={} valor={} particion={} offset={}",
                lectura.equipoId(), lectura.tipo(), lectura.valor(), registro.partition(), registro.offset());
    }
}