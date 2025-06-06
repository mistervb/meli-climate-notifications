package com.mercadolibre.itarc.climatehub_ms_notification.repository;

import com.mercadolibre.itarc.climatehub_ms_notification.model.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
}
