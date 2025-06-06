package com.mercadolibre.itarc.climatehub_ms_notification.model.dto;

import com.mercadolibre.itarc.climatehub_ms_notification.constants.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatusDTO {
    private NotificationStatus status;
    private String message;
}