package com.mercadolibre.itarc.climatehub_ms_notification.validator.notification;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.validator.BaseValidationHandler;
import com.mercadolibre.itarc.climatehub_ms_notification.exception.BusinessException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class TimeFormatValidator extends BaseValidationHandler {
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");

    @Override
    public void validate(NotificationRequest request) {
        if (request.time() != null) {
            validateTimeFormat(request.time());
            validateTimeRange(request.time());
        }
        validateNext(request);
    }

    private void validateTimeFormat(String time) {
        if (!TIME_PATTERN.matcher(time).matches()) {
            throw new BusinessException("time must be in HH:mm format (24-hour)");
        }
    }

    private void validateTimeRange(String time) {
        try {
            LocalTime parsedTime = LocalTime.parse(time);
            if (parsedTime.isBefore(LocalTime.of(6, 0)) || parsedTime.isAfter(LocalTime.of(22, 0))) {
                throw new BusinessException("time must be between 06:00 and 22:00");
            }
        } catch (DateTimeParseException e) {
            throw new BusinessException("Invalid time format: " + e.getMessage());
        }
    }
} 