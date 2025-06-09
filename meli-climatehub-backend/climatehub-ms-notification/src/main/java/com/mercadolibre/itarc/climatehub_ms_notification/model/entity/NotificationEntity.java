package com.mercadolibre.itarc.climatehub_ms_notification.model.entity;

import com.mercadolibre.itarc.climatehub_ms_notification.constants.NotificationStatus;
import com.mercadolibre.itarc.climatehub_ms_notification.constants.ScheduleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationId;
    
    private UUID userId;
    private Integer cityId;

    @Enumerated(EnumType.STRING)
    private ScheduleType type; // ONCE, DAILY, WEEKLY, CUSTOM

    private String cityName;
    private String uf;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status; // PENDING, SENT, FAILED
    private Integer dayOfWeek;
    private String time;
    private String cronExpression;
    private LocalDateTime nextExecution;
    private LocalDateTime endDate;
}
