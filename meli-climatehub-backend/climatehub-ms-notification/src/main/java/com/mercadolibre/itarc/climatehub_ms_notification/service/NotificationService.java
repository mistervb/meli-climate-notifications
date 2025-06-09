package com.mercadolibre.itarc.climatehub_ms_notification.service;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationDetailsResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationStatusDTO;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    NotificationResponse scheduleNotification(NotificationRequest request);
    List<NotificationDetailsResponse> getAllNotifications(UUID userId);
    NotificationDetailsResponse getNotificationById(UUID userId, UUID notificationId);
    void updateStatus(UUID notificationId, NotificationStatusDTO statusDTO);
}
