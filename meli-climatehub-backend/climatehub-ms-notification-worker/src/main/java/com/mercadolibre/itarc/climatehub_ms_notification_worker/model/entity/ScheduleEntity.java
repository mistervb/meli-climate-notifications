package com.mercadolibre.itarc.climatehub_ms_notification_worker.model.entity;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleStatus;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID notificationId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String cityId;

    @Column(nullable = false)
    private String cityName;

    @Column(nullable = false, length = 2)
    private String uf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType;

    @Column(nullable = false)
    private LocalTime scheduleTime;

    @Column
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalDateTime nextExecution;

    @Column
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.ACTIVE;

    @Column(length = 1000)
    private String authToken;
}

