package com.mercadolibre.itarc.climatehub_ms_notification_worker.constants;

public enum ScheduleType {
    ONCE,       // Único
    DAILY,      // Diário (horário fixo)
    WEEKLY,     // Semanal (ex: toda segunda)
    CUSTOM ;    // Cron personalizado (ex: fins de semana)

    public static ScheduleType fromString(String type) {
        try {
            return ScheduleType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid schedule type: " + type);
        }
    }
}
