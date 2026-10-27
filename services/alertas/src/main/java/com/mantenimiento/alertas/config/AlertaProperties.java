package com.mantenimiento.alertas.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alertas")
public record AlertaProperties(Map<String, Regla> reglas) {

    public AlertaProperties {
        reglas = reglas == null ? Map.of() : reglas;
    }

    
    public record Regla(double umbral, double critico) {}
}