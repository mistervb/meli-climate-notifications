package com.mercadolibre.itarc.climatehub_ms_notification_worker.feign.client;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.dto.NotificationStatusDTO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.dto.NotificationDTO;

@FeignClient(
    name = "climatehub-ms-notification"
)
public interface NotificationFeignClient {
    
    @GetMapping("/notification/{notificationId}")
    NotificationDTO getNotification(@PathVariable UUID notificationId);

    @PutMapping("/notification/{notificationId}/status")
    void updateStatus(@PathVariable UUID notificationId, @RequestBody NotificationStatusDTO status);
}