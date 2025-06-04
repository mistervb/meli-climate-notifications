package com.mercadolibre.itarc.climatehub_service_discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class ClimatehubServiceDiscoveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClimatehubServiceDiscoveryApplication.class, args);
	}

}
