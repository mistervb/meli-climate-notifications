package com.mercadolibre.itarc.climatehub_ms_notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ClimatehubMsNotificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClimatehubMsNotificationApplication.class, args);
	}

}
