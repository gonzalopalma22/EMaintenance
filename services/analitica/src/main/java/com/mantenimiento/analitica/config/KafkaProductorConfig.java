package com.mantenimiento.analitica.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.mantenimiento.analitica.dto.Resumen;
import com.mantenimiento.analitica.messaging.ResumenSerializer;

@Configuration
public class KafkaProductorConfig {

    /** Plantilla de texto: la usa la DLT para reenviar el mensaje fallido tal como llegó. */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> fabrica) {
        return new KafkaTemplate<>(fabrica);
    }

    /**
     * Plantilla tipada para los resúmenes: mismas propiedades del productor del application.yml
     * (acks=all, idempotencia, compresión), pero con {@link ResumenSerializer} como serializador del valor.
     */
    @Bean
    public KafkaTemplate<String, Resumen> resumenKafkaTemplate(ProducerFactory<String, String> base) {
        Map<String, Object> config = new HashMap<>(base.getConfigurationProperties());
        config.put(ProducerConfig.CLIENT_ID_CONFIG, "analitica-producer-resumen");
        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(config, new StringSerializer(), new ResumenSerializer()));
    }
}