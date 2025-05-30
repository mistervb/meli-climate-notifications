package com.mercadolibre.itarc.climatehub_ms_notification.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(UUID notificationId, LocalDateTime nextExecution)
{ }
