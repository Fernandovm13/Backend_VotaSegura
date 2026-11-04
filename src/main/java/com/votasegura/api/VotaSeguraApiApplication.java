package com.votasegura.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class VotaSeguraApiApplication {

    public static void main(String[] args) {
        // Sincronizar zona horaria de la JVM con México (UTC-6)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
        SpringApplication.run(VotaSeguraApiApplication.class, args);
    }
}