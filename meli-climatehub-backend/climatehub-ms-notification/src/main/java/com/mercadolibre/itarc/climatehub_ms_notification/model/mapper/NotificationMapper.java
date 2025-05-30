package com.mercadolibre.itarc.climatehub_ms_notification.model.mapper;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.model.entity.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "cityId", ignore = true)
    @Mapping(target = "status", ignore = true) // adiciona depois
    @Mapping(target = "nextExecution", ignore = true)
    @Mapping(target = "cronExpression", ignore = true)
    @Mapping(target = "type", source = "scheduleType")
    @Mapping(target = "time", source = "time")
    @Mapping(target = "dayOfWeek", source = "dayOfWeek")
    @Mapping(target = "endDate", source = "endDate")
    NotificationEntity toEntity(NotificationRequest request);

    NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);

    @Mapping(target = "notificationId", source = "notificationId")
    @Mapping(target = "nextExecution", source = "nextExecution")
    NotificationResponse toResponse(NotificationEntity entity);
}
