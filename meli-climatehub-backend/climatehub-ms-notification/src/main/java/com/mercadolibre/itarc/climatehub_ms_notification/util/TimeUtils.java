package com.mercadolibre.itarc.climatehub_ms_notification.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;

import lombok.Getter;

public class TimeUtils {
    public static TimeAdjusted adjustTime(NotificationRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ZoneId zoneId = ZoneId.of("America/Sao_Paulo");
        ZonedDateTime zonedNow = now.atZone(ZoneId.systemDefault()).withZoneSameInstant(zoneId);

        LocalDateTime baseTime = zonedNow.toLocalDateTime()
            .withHour(LocalTime.parse(request.time()).getHour())
            .withMinute(LocalTime.parse(request.time()).getMinute())
            .withSecond(0)
            .withNano(0);
            
        return new TimeAdjusted(
            baseTime,
            zonedNow,
            zoneId
        );
    }

    @Getter
    public static class TimeAdjusted {    
        private LocalDateTime baseTime;
        private ZonedDateTime zonedNow;
        private ZoneId zoneId;
        
        private TimeAdjusted() { }

        private TimeAdjusted(LocalDateTime baseTime, ZonedDateTime zonedNow, ZoneId zoneId) {
            this.baseTime = baseTime;
            this.zonedNow = zonedNow;
            this.zoneId   = zoneId;
        }
    }
}
