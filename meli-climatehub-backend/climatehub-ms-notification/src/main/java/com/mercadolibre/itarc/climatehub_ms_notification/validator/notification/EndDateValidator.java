package com.mercadolibre.itarc.climatehub_ms_notification.validator.notification;

import com.mercadolibre.itarc.climatehub_ms_notification.constants.ScheduleType;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.validator.BaseValidationHandler;
import com.mercadolibre.itarc.climatehub_ms_notification.exception.BusinessException;
import java.time.LocalDateTime;

public class EndDateValidator extends BaseValidationHandler {
    @Override
    public void validate(NotificationRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("endDate must be a future date/time");
        }

        if (request.endDate() != null && request.scheduleType() != ScheduleType.ONCE) {
            LocalDateTime maxDate = LocalDateTime.now().plusYears(1);
            if (request.endDate().isAfter(maxDate)) {
                throw new BusinessException("endDate cannot be more than 1 year in the future");
            }
        }
        validateNext(request);
    }
} 