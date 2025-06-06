package com.mercadolibre.itarc.climatehub_ms_user.service;

import java.util.UUID;

public interface RedisOptOutService {
    void setOptOut(UUID userId, boolean optOut);
    boolean isOptOut(UUID userId);
    void removeOptOut(UUID userId);
} 