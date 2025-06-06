package com.mercadolibre.itarc.climatehub_ms_user.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCreatedDTO(UUID userId, String username, LocalDateTime createdAt, LocalDateTime updatedAt) {
    
}
