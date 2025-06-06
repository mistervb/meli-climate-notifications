package com.mercadolibre.itarc.climatehub_ms_notification_worker.producer;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.dto.WeatherNotificationDTO;

@Component
public class NotificationSSEProducer {
    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter messageConverter;

    private static final String SSE_NOTIFICATION_QUEUE = "sse-notification-queue";

    public NotificationSSEProducer(
        RabbitTemplate rabbitTemplate,
        MessageConverter messageConverter
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageConverter = messageConverter;
    }

    public void sendToProcess(WeatherNotificationDTO request, String token) {
        MessageProperties properties = new MessageProperties();
        if (token != null) {
            properties.setHeader("Authorization", token);
        }

        Message message = messageConverter.toMessage(request, properties);
        rabbitTemplate.send(SSE_NOTIFICATION_QUEUE, message);
        System.out.println("📤 Enviado para fila: " + request);
    }
}
