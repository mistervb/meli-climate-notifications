package com.mercadolibre.itarc.climatehub_ms_user.service.user;

import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserCreatedDTO;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserPayload;

import java.util.UUID;

public interface UserService {
    UserCreatedDTO registerUser(UserPayload payload);
    void setNotificationOptOut(UUID userId, boolean optOut);
    UUID getUserIdByEmail(String email);
}
