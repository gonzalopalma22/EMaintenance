package com.mantenimiento.analitica.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.mantenimiento.analitica.service.AgregadorService;

@Configuration
public class KafkaErrorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorConfig.class);

    /**
     * Reintenta dos veces los fallos transitorios y, si el mensaje sigue fallando,
     * lo publica en el tópico "<original>.DLT" y deja el motivo en el log.
     * Los mensajes mal formados (IllegalArgumentException) van directo a la DLT.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template,
                                                 AgregadorService agregador) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

        DefaultErrorHandler handler = new DefaultErrorHandler((registro, ex) -> {
            Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
            log.error("Mensaje enviado a la DLT. topic={} particion={} offset={} motivo={}",
                    registro.topic(), registro.partition(), registro.offset(), causa.getMessage());
            agregador.registrarDescartada();
            recoverer.accept(registro, ex);
        }, new FixedBackOff(1000L, 2L));

        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}