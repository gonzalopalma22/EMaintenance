package com.mantenimiento.admin_rabbitmq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ManagementClientConfig {

    @Bean
    public RestClient managementClient(
            @Value("${rabbitmq-management.url}") String url,
            @Value("${spring.rabbitmq.username}") String user,
            @Value("${spring.rabbitmq.password}") String pass) {
        return RestClient.builder()
                .baseUrl(url + "/api")
                .defaultHeaders(h -> h.setBasicAuth(user, pass))
                .build();
    }
}