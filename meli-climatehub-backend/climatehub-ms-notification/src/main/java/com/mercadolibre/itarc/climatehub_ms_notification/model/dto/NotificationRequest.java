package com.mercadolibre.itarc.climatehub_ms_notification.model.dto;

import com.mercadolibre.itarc.climatehub_ms_notification.constants.ScheduleType;

import java.time.LocalDateTime;

public record NotificationRequest(
        String cityName,
        String uf,
        ScheduleType scheduleType,
        String time,
        Integer dayOfWeek,
        LocalDateTime executeAt,
        LocalDateTime endDate
) { }
