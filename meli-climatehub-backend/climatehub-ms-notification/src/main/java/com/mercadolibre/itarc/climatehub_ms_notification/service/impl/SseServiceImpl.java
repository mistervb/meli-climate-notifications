package com.mercadolibre.itarc.climatehub_ms_notification.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.mercadolibre.itarc.climatehub_ms_notification.service.SseService;

import jakarta.annotation.PreDestroy;

@Service
public class SseServiceImpl implements SseService {
    private static final Logger logger = LoggerFactory.getLogger(SseServiceImpl.class);
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private static final long CLEANUP_INTERVAL = 60; // 60 segundos
    private static final int MAX_EMITTERS_PER_USER = 3;

    public SseServiceImpl() {
        // Inicia o heartbeat a cada 15 segundos
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 0, 15, TimeUnit.SECONDS);
        
        // Inicia a limpeza de emitters inativos a cada 60 segundos
        cleanupExecutor.scheduleAtFixedRate(this::cleanupInactiveEmitters, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        heartbeatExecutor.shutdown();
        cleanupExecutor.shutdown();
        emitters.clear();
    }

    private void cleanupInactiveEmitters() {
        logger.debug("Iniciando limpeza de emitters inativos");
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data("")
                        .id(String.valueOf(System.currentTimeMillis())));
            } catch (Exception e) {
                logger.debug("Removendo emitter inativo para usuário: {}", userId);
                removeEmitter(userId);
            }
        });
    }

    private void sendHeartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping")
                        .id(String.valueOf(System.currentTimeMillis()))
                        .reconnectTime(3000L));
            } catch (IOException e) {
                logger.debug("Erro ao enviar heartbeat para usuário: {}. Removendo emitter.", userId);
                removeEmitter(userId);
            }
        });
    }

    public SseEmitter createEmitter(UUID userId) {
        // Remove emitters antigos se exceder o limite por usuário
        long userEmitterCount = emitters.entrySet().stream()
                .filter(entry -> entry.getKey().equals(userId))
                .count();
                
        if (userEmitterCount >= MAX_EMITTERS_PER_USER) {
            logger.warn("Usuário {} excedeu o limite de emitters. Removendo emitters antigos.", userId);
            emitters.entrySet().removeIf(entry -> entry.getKey().equals(userId));
        }

        SseEmitter emitter = new SseEmitter(180000L); // 3 minutos
        
        emitter.onCompletion(() -> {
            logger.debug("Emitter completado para usuário: {}", userId);
            removeEmitter(userId);
        });
        
        emitter.onTimeout(() -> {
            logger.debug("Emitter timeout para usuário: {}", userId);
            removeEmitter(userId);
        });
        
        emitter.onError(e -> {
            logger.error("Erro no emitter para usuário: {}", userId, e);
            removeEmitter(userId);
        });
        
        emitters.put(userId, emitter);
        return emitter;
    }

    public void sendNotification(UUID userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("weather-notification")
                        .data(data)
                        .id(String.valueOf(System.currentTimeMillis()))
                        .reconnectTime(3000L));
            } catch (IOException e) {
                logger.error("Erro ao enviar notificação para usuário: {}", userId, e);
                removeEmitter(userId);
            }
        }
    }

    public void removeEmitter(UUID userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                logger.error("Erro ao completar emitter para usuário: {}", userId, e);
            }
        }
    }

    public boolean hasEmitter(UUID userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return false;
        }
        
        try {
            // Tenta enviar um ping para verificar se o emitter ainda está ativo
            emitter.send(SseEmitter.event()
                    .name("ping")
                    .data("")
                    .id(String.valueOf(System.currentTimeMillis())));
            return true;
        } catch (Exception e) {
            logger.debug("Emitter inativo detectado para usuário: {}", userId);
            removeEmitter(userId);
            return false;
        }
    }
}
