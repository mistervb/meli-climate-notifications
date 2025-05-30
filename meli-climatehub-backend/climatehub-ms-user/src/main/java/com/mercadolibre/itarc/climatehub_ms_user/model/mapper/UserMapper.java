package com.mercadolibre.itarc.climatehub_ms_user.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserCreatedDTO;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserDTO;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserPayload;
import com.mercadolibre.itarc.climatehub_ms_user.model.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "passwordHashed", source = "passwordHashed")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "notificationOptOut", constant = "true")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    UserEntity toEntity(UserPayload payload);


    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "username", source = "username")
    UserDTO toData(UserEntity entity);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserCreatedDTO toCreateData(UserEntity entity);
}
