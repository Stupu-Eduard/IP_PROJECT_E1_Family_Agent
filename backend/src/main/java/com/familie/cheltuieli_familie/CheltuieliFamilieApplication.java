package com.familie.cheltuieli_familie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
public class CheltuieliFamilieApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheltuieliFamilieApplication.class, args);
	}

}
