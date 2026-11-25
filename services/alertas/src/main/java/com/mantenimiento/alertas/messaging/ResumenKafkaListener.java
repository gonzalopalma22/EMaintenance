package com.mantenimiento.alertas.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.mantenimiento.alertas.dto.ResumenEvent;
import com.mantenimiento.alertas.service.TendenciaService;

/** Capa de mensajería: recibe el resumen, lo convierte a DTO y delega la lógica en el servicio de tendencias. */
@Component
public class ResumenKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(ResumenKafkaListener.class);

    private final TendenciaService tendencias;

    public ResumenKafkaListener(TendenciaService tendencias) {
        this.tendencias = tendencias;
    }

    @KafkaListener(topics = "${kafka.topic-resumen}")
    public void consumir(ConsumerRecord<String, String> registro) {
        ResumenEvent resumen = ResumenEventMapper.desdeJson(registro.value());

        tendencias.evaluar(resumen);
        log.debug("Resumen consumido. equipo={} tipo={} promedio={} particion={} offset={}",
                resumen.equipoId(), resumen.tipo(), resumen.promedio(), registro.partition(), registro.offset());
    }
}