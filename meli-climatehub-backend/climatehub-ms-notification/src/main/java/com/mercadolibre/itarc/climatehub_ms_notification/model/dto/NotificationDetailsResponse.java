package com.mercadolibre.itarc.climatehub_ms_notification.model.dto;

import com.mercadolibre.itarc.climatehub_ms_notification.constants.NotificationStatus;
import com.mercadolibre.itarc.climatehub_ms_notification.constants.ScheduleType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDetailsResponse(
        UUID notificationId,
        ScheduleType type,
        NotificationStatus status,
        String cityName,
        String uf,
        LocalDateTime nextExecution
) { }
