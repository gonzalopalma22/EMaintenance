package com.mantenimiento.ordenes_trabajo.config;

import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter messageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper mapper = new DefaultJacksonJavaTypeMapper();
        mapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        mapper.setTrustedPackages("com.mantenimiento.ordenes_trabajo.dto");
        converter.setJavaTypeMapper(mapper);
        return converter;
    }
}