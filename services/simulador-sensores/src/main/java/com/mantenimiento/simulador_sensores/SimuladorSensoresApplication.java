package com.mantenimiento.simulador_sensores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SimuladorSensoresApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimuladorSensoresApplication.class, args);
    }

}