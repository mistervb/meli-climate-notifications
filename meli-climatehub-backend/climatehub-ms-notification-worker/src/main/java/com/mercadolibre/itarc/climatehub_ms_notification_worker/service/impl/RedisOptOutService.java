package com.mercadolibre.itarc.climatehub_ms_notification_worker.service.impl;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RedisOptOutService {

    private final RedisTemplate<String, Boolean> redisTemplate;
    private static final String OPT_OUT_PREFIX = "user:optout:";

    public RedisOptOutService(RedisTemplate<String, Boolean> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isOptOut(UUID userId) {
        String key = OPT_OUT_PREFIX + userId;
        Boolean result = redisTemplate.opsForValue().get(key);
        return result != null && result;
    }
}
