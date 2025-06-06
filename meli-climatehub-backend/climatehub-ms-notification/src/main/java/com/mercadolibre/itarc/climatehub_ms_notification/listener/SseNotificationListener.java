package com.mercadolibre.itarc.climatehub_ms_notification.listener;

import com.mercadolibre.itarc.climatehub_ms_notification.config.RabbitMQConfig;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.WeatherNotificationDTO;
import com.mercadolibre.itarc.climatehub_ms_notification.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SseNotificationListener {

    private final SseService sseService;

    public SseNotificationListener(SseService sseService) {
        this.sseService = sseService;
    }

    @RabbitListener(queues = RabbitMQConfig.SSE_NOTIFICATION_QUEUE)
    public void receiveNotification(WeatherNotificationDTO notification) {
        log.info("📨 Recebida notificação para envio via SSE: {}", notification);
        
        if (sseService.hasEmitter(notification.getUserId())) {
            sseService.sendNotification(notification.getUserId(), notification);
            log.info("✅ Notificação enviada com sucesso via SSE para o usuário: {}", notification.getUserId());
        } else {
            log.warn("⚠️ Usuário não está conectado via SSE: {}", notification.getUserId());
        }
    }
} 