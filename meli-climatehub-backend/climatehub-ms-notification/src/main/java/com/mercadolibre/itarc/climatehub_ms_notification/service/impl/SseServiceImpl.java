package com.mercadolibre.itarc.climatehub_ms_notification.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mercadolibre.itarc.climatehub_ms_notification.service.SseService;

@Service
public class SseServiceImpl implements SseService {
     private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(UUID userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));
        
        emitters.put(userId, emitter);
        return emitter;
    }

    public void sendNotification(UUID userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("weather-notification")
                        .data(data));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }

    public void removeEmitter(UUID userId) {
        emitters.remove(userId);
    }

    public boolean hasEmitter(UUID userId) {
        return emitters.containsKey(userId);
    }
}
