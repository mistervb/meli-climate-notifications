package com.mercadolibre.itarc.climatehub_ms_notification.validator.notification;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;

public interface NotificationValidationHandler {
    void setNext(NotificationValidationHandler handler);
    void validate(NotificationRequest request);
} 