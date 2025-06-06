package com.mercadolibre.itarc.climatehub_ms_notification.validator.notification;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.exception.BusinessException;
import com.mercadolibre.itarc.climatehub_ms_notification.validator.BaseValidationHandler;

import java.util.regex.Pattern;

public class RequiredFieldsValidator extends BaseValidationHandler {
    private static final Pattern UF_PATTERN = Pattern.compile("^[A-Z]{2}$");

    @Override
    public void validate(NotificationRequest request) {
        if (request.cityName() == null || request.cityName().isBlank()) {
            throw new BusinessException("City name cannot be null or empty");
        }

        if (request.uf() == null || request.uf().isBlank()) {
            throw new BusinessException("UF cannot be null or empty");
        } else if (!UF_PATTERN.matcher(request.uf()).matches()) {
            throw new BusinessException("UF must be 2 uppercase letters (e.g., 'SP')");
        }

        if (request.scheduleType() == null) {
            throw new BusinessException("Schedule type cannot be null");
        }

        validateNext(request);
    }
}