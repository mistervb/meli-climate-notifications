package com.mercadolibre.itarc.climatehub_ms_notification_worker.repository;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, UUID> {
    @Query(
        " SELECT s FROM ScheduleEntity s WHERE s.status = 'ACTIVE' " +
        "   AND (s.nextExecution BETWEEN :startTime AND :endTime   " +
        "    OR s.nextExecution <= :now)                           " +
        "   AND (s.endDate IS NULL OR s.endDate > :now)            " 
    )
    List<ScheduleEntity> findPendingNotificationsWithTolerance(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("now") LocalDateTime now);
}