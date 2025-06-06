package com.mercadolibre.itarc.climatehub_ms_notification.controller;

import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationStatusDTO;
import com.mercadolibre.itarc.climatehub_ms_notification.service.NotificationService;
import com.mercadolibre.itarc.climatehub_ms_notification.service.SseService;
import com.mercadolibre.itarc.climatehub_ms_notification.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/notification")
@Slf4j
public class NotificationController {
    private final NotificationService notificationService;
    private final SseService sseService;
    private final TokenService tokenService;

    public NotificationController(NotificationService notificationService, SseService sseService, TokenService tokenService) {
        this.notificationService = notificationService;
        this.sseService = sseService;
        this.tokenService = tokenService;
    }

    @PostMapping("/schedule")
    public ResponseEntity<NotificationResponse> scheduleNotification(@RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.scheduleNotification(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{notificationId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID notificationId,
            @RequestBody NotificationStatusDTO statusDTO) {
        log.info("Atualizando status da notificação {}: {}", notificationId, statusDTO);
        notificationService.updateStatus(notificationId, statusDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        String userId = tokenService.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("Token inválido ou ausente");
        }
        
        SseEmitter emitter = sseService.createEmitter(UUID.fromString(userId));
        
        // Envia um evento inicial para confirmar a conexão
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Conexão SSE estabelecida com sucesso!"));
        } catch (Exception e) {
            log.error("Erro ao enviar evento de conexão SSE", e);
        }
        
        return emitter;
    }
}
