package com.mantenimiento.alertas.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alertas")
public record AlertaProperties(Map<String, Regla> reglas, Duration enfriamiento) {

    public AlertaProperties {
        reglas = reglas == null ? Map.of() : reglas;
        enfriamiento = enfriamiento == null ? Duration.ofMinutes(10) : enfriamiento;
    }

    public record Regla(double umbral, double critico) {}
}