package com.mercadolibre.itarc.climatehub_ms_notification.model.dto;

import com.mercadolibre.itarc.climatehub_ms_notification.constants.ScheduleType;
import com.mercadolibre.itarc.climatehub_ms_notification.model.redis.CityCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CityRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String cityName;
    private String uf;
    private ScheduleType scheduleType; // enum: DAILY, WEEKLY, ONCE
    private String time; // formato HH:mm
    private LocalDateTime executeAt; // usado apenas para ONCE
    private DayOfWeek dayOfWeek; // usado apenas para WEEKLY
    private LocalDateTime endDate;
    private UUID notificationId;
    private UUID userId;
    private CityCache cityInfo;
}
