package com.mercadolibre.itarc.climatehub_ms_notification.validator.notification;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.validator.BaseValidationHandler;
import com.mercadolibre.itarc.climatehub_ms_notification.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ScheduleTypeValidator extends BaseValidationHandler {
    @Override
    public void validate(NotificationRequest request) {
        switch (request.scheduleType()) {
            case ONCE -> validateOnceSchedule(request);
            case DAILY -> validateDailySchedule(request);
            case WEEKLY -> validateWeeklySchedule(request);
            case CUSTOM -> throw new IllegalArgumentException("CUSTOM is not supported in this version.");
        }
        validateNext(request);
    }

    private void validateOnceSchedule(NotificationRequest request) {
        if (request.executeAt() == null) {
            throw new BusinessException("executeAt is required for ONCE schedule type");
        }

        // Obtém o horário atual em UTC
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        if (request.executeAt().isBefore(utcNow)) {
            throw new BusinessException("executeAt must be a future date/time");
        }

        if (request.time() != null || request.dayOfWeek() != null) {
            throw new BusinessException("ONCE schedule should only contain executeAt");
        }
    }

    private void validateDailySchedule(NotificationRequest request) {
        if (request.time() == null) {
            throw new BusinessException("time is required for DAILY schedule type");
        }

        if (request.executeAt() != null || request.dayOfWeek() != null) {
            throw new BusinessException("DAILY schedule should only contain time");
        }
    }

    private void validateWeeklySchedule(NotificationRequest request) {
        if (request.time() == null) {
            throw new BusinessException("time is required for WEEKLY schedule type");
        }

        if (request.dayOfWeek() == null) {
            throw new BusinessException("dayOfWeek is required for WEEKLY schedule type");
        } else if (request.dayOfWeek() < 1 || request.dayOfWeek() > 7) {
            throw new BusinessException("dayOfWeek must be between 1 (Monday) and 7 (Sunday)");
        }

        if (request.executeAt() != null) {
            throw new BusinessException("WEEKLY schedule should only contain time and dayOfWeek");
        }
    }

    private void validateCustomSchedule(NotificationRequest request) {
        if (request.executeAt() != null || request.time() != null || request.dayOfWeek() != null) {
            throw new BusinessException("CUSTOM schedule should only contain cron expression");
        }
    }
} 