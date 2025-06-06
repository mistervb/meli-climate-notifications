package com.mercadolibre.itarc.climatehub_ms_notification.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherNotificationDTO {
    private UUID userId;
    private UUID notificationId;
    private String cityName;
    private String uf;
    private LocalDate date;
    private Integer minTemp;
    private Integer maxTemp;
    private String message;
} 