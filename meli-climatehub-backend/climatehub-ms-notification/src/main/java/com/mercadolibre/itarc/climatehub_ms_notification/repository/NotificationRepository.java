package com.mercadolibre.itarc.climatehub_ms_notification.repository;

import com.mercadolibre.itarc.climatehub_ms_notification.model.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    @Query("SELECT n FROM NotificationEntity n WHERE n.userId = :userId")
    List<NotificationEntity> getAllByUserId(@Param(value = "userId") UUID userId);

    @Query("SELECT n FROM NotificationEntity n WHERE n.notificationId = :notificationId AND n.userId = :userId")
    Optional<NotificationEntity> findByNotificationIdAndUserId(
            @Param(value = "notificationId") UUID notificationId,
            @Param(value = "userId") UUID userId
    );
}
