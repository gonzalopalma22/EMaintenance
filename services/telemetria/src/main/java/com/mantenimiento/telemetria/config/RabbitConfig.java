package com.mantenimiento.telemetria.config;

import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mantenimiento.telemetria.exception.LecturaInvalidaException;

@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public ConditionalRejectingErrorHandler errorHandler() {
        FatalExceptionStrategy strategy = new ConditionalRejectingErrorHandler.DefaultExceptionStrategy() {
            @Override
            public boolean isFatal(Throwable t) {
                Throwable cause = t.getCause();
                return cause instanceof LecturaInvalidaException || super.isFatal(t);
            }
        };
        return new ConditionalRejectingErrorHandler(strategy);
    }
}