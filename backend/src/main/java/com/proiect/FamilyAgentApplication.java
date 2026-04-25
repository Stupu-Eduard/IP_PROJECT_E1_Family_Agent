package com.proiect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FamilyAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyAgentApplication.class, args);
    }

}
