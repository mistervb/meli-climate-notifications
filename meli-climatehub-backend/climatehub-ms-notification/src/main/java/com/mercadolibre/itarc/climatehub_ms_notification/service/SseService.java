package com.mercadolibre.itarc.climatehub_ms_notification.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public interface SseService {
   SseEmitter createEmitter(UUID userId);
   void sendNotification(UUID userId, Object data);
   void removeEmitter(UUID userId);
   boolean hasEmitter(UUID userId);
} 