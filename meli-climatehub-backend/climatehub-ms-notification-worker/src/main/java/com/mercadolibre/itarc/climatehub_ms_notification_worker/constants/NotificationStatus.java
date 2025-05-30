package com.mercadolibre.itarc.climatehub_ms_notification_worker.constants;

public enum NotificationStatus {
    PENDING,
    EXECUTED,
    FAILED;

    public static NotificationStatus fromString(String status) {
        try {
            return NotificationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid notification status: " + status);
        }
    }
}
