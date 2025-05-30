package com.mercadolibre.itarc.climatehub_ms_notification_worker.model.dto;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private UUID id;
    private UUID userId;
    private String cityName;
    private String uf;
    private NotificationStatus status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 