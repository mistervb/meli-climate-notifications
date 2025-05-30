package com.mercadolibre.itarc.climatehub_ms_notification.producer;

import com.mercadolibre.itarc.climatehub_ms_notification.config.RequestInterceptor;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.CityRequestDTO;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {
    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter messageConverter;

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageConverter = rabbitTemplate.getMessageConverter();
    }

    public void sendToProcess(CityRequestDTO request) {
        MessageProperties properties = new MessageProperties();
        String token = RequestInterceptor.getCurrentToken();
        if (token != null) {
            properties.setHeader("Authorization", token);
        }

        Message message = messageConverter.toMessage(request, properties);
        rabbitTemplate.send("notification-processing-queue", message);
        System.out.println("📤 Enviado para fila: " + request);
    }
}
