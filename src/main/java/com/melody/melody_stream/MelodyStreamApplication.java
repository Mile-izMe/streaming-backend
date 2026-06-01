package com.melody.melody_stream;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

import java.util.TimeZone;

@SpringBootApplication
@EntityScan(basePackages = "com.melody.melody_stream")
public class MelodyStreamApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		SpringApplication.run(MelodyStreamApplication.class, args);
	}

}
