package com.mercadolibre.itarc.climatehub_ms_notification.validator.notification;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;

public class NotificationValidator {
    private static NotificationValidationHandler createValidationChain() {
        NotificationValidationHandler requiredFieldsValidator = new RequiredFieldsValidator();
        NotificationValidationHandler scheduleTypeValidator = new ScheduleTypeValidator();
        NotificationValidationHandler timeFormatValidator = new TimeFormatValidator();
        NotificationValidationHandler endDateValidator = new EndDateValidator();

        requiredFieldsValidator.setNext(scheduleTypeValidator);
        scheduleTypeValidator.setNext(timeFormatValidator);
        timeFormatValidator.setNext(endDateValidator);

        return requiredFieldsValidator;
    }

    public static void validateNotificationRequest(NotificationRequest request) {
        NotificationValidationHandler validationChain = createValidationChain();
        validationChain.validate(request);
    }
}