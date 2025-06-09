package com.mercadolibre.itarc.climatehub_ms_notification.model.mapper;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationDetailsResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.model.entity.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "nextExecution", ignore = true)
    @Mapping(target = "cronExpression", ignore = true)
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "cityId", ignore = true)
    @Mapping(target = "cityName", source = "cityName")
    @Mapping(target = "uf", source = "uf")
    @Mapping(target = "type", source = "scheduleType")
    @Mapping(target = "time", source = "time")
    @Mapping(target = "dayOfWeek", source = "dayOfWeek")
    @Mapping(target = "endDate", source = "endDate")
    NotificationEntity toEntity(NotificationRequest request);

    @Mapping(target = "notificationId", source = "notificationId")
    @Mapping(target = "nextExecution", source = "nextExecution")
    NotificationResponse toResponse(NotificationEntity entity);

    @Mapping(target = "notificationId", source = "notificationId")
    @Mapping(target = "nextExecution", source = "nextExecution")
    @Mapping(target = "cityName", source = "cityName")
    @Mapping(target = "uf", source = "uf")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "status", source = "status")
    NotificationDetailsResponse toDetails(NotificationEntity entity);
}
