package com.mercadolibre.itarc.climatehub_ms_notification.validator;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.validator.notification.NotificationValidationHandler;

public abstract class BaseValidationHandler implements NotificationValidationHandler {
    private NotificationValidationHandler nextHandler;

    @Override
    public void setNext(NotificationValidationHandler handler) {
        this.nextHandler = handler;
    }

    protected void validateNext(NotificationRequest request) {
        if (nextHandler != null) {
            nextHandler.validate(request);
        }
    }
} 