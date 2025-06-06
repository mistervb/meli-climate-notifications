package com.mercadolibre.itarc.climatehub_ms_user.service.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.mercadolibre.itarc.climatehub_ms_user.service.RedisOptOutService;

@Service
public class RedisOptOutServiceImpl implements RedisOptOutService {
    private static final Logger log = LoggerFactory.getLogger(RedisOptOutServiceImpl.class);
    private final RedisTemplate<String, Boolean> redisTemplate;
    private static final String OPT_OUT_PREFIX = "user:optout:";

    public RedisOptOutServiceImpl(RedisTemplate<String, Boolean> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setOptOut(UUID userId, boolean optOut) {
        String key = OPT_OUT_PREFIX + userId;
        try {
            redisTemplate.opsForValue().set(key, optOut);
            log.debug("✅ Opt-out definido para usuário {}: {}", userId, optOut);
        } catch (Exception e) {
            log.error("❌ Erro ao definir opt-out para usuário {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean isOptOut(UUID userId) {
        String key = OPT_OUT_PREFIX + userId;
        try {
            Boolean result = redisTemplate.opsForValue().get(key);
            log.debug("🔍 Verificando opt-out para usuário {}: {}", userId, result);
            return result != null && result;
        } catch (Exception e) {
            log.error("❌ Erro ao verificar opt-out para usuário {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void removeOptOut(UUID userId) {
        String key = OPT_OUT_PREFIX + userId;
        try {
            redisTemplate.delete(key);
            log.debug("🗑️ Opt-out removido para usuário {}", userId);
        } catch (Exception e) {
            log.error("❌ Erro ao remover opt-out para usuário {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
} 