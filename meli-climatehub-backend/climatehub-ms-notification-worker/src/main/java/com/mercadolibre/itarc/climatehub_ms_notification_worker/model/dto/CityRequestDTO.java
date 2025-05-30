package com.mercadolibre.itarc.climatehub_ms_notification_worker.model.dto;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CityRequestDTO {
    private String cityName;
    private String uf;
    private ScheduleType scheduleType;
    private String time;
    private LocalDateTime executeAt;
    private DayOfWeek dayOfWeek;
    private LocalDateTime endDate;
    private UUID notificationId;
    private UUID userId;
}