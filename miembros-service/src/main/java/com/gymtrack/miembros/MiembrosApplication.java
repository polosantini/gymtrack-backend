package com.gymtrack.miembros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MiembrosApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiembrosApplication.class, args);
    }
}