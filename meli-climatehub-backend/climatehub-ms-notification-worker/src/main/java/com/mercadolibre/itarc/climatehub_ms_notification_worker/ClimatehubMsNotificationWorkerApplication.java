package com.mercadolibre.itarc.climatehub_ms_notification_worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ClimatehubMsNotificationWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClimatehubMsNotificationWorkerApplication.class, args);
	}

}
