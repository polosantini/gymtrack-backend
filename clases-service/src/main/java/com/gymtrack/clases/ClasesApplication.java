package com.gymtrack.clases;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class ClasesApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClasesApplication.class, args);
    }
}