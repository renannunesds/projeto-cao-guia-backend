package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProjetoCaoGuiaBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjetoCaoGuiaBackendApplication.class, args);
    }
}