package com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeatherCache implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Integer cityId;
    private LocalDate date;
    private Double minTemp;
    private Double maxTemp;
    private Double waveHeight;
    private Instant expiresAt;
}
