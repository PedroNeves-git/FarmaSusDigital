package br.com.fiap.farmasusdigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FarmaSusDigitalApplication {

	public static void main(String[] args) {
		SpringApplication.run(FarmaSusDigitalApplication.class, args);
	}

}
