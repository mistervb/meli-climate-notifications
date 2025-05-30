package com.mercadolibre.itarc.climatehub_ms_notification_worker.config;

import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@Configuration
@EnableFeignClients(
    basePackages = "com.mercadolibre.itarc.climatehub_ms_notification_worker.feign",
    defaultConfiguration = FeignConfig.class
)
public class FeignConfig {
    private static final ThreadLocal<String> authTokenHolder = new ThreadLocal<>();

    @Bean
    public Retryer retryer() {
        // Retry 3 times, starting with 100ms delay and exponentially increasing to max 1s
        return new Retryer.Default(100, 1000, 3);
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String token = authTokenHolder.get();
            if (token != null) {
                requestTemplate.header("Authorization", token);
            }
        };
    }

    public static void setAuthToken(String token) {
        authTokenHolder.set(token);
    }

    public static void clearAuthToken() {
        authTokenHolder.remove();
    }
} 