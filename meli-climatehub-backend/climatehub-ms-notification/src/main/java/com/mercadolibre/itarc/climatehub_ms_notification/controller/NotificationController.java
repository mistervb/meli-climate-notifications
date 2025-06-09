package com.mercadolibre.itarc.climatehub_ms_notification.controller;

import com.mercadolibre.itarc.climatehub_ms_notification.exception.InvalidPathException;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationDetailsResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationStatusDTO;
import com.mercadolibre.itarc.climatehub_ms_notification.service.NotificationService;
import com.mercadolibre.itarc.climatehub_ms_notification.service.SseService;
import com.mercadolibre.itarc.climatehub_ms_notification.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notification")
public class NotificationController {
    private static final int MAX_PATH_LENGTH = 100; // Tamanho máximo permitido para o caminho
    
    @ModelAttribute
    public void validatePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path != null && path.length() > MAX_PATH_LENGTH) {
            throw new InvalidPathException("Caminho da requisição muito longo. Possível recursão detectada.");
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
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
        logger.info("Controller - Iniciando agendamento de notificação: {}", request);
        try {
            NotificationResponse response = notificationService.scheduleNotification(request);
            logger.info("Controller - Notificação agendada com sucesso: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Controller - Erro ao agendar notificação: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping(value = "/{notificationId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NotificationStatusDTO> updateStatus(
            @PathVariable UUID notificationId,
            @RequestBody NotificationStatusDTO statusDTO) {
        logger.info("Atualizando status da notificação {}: {}", notificationId, statusDTO);
        notificationService.updateStatus(notificationId, statusDTO);
        return ResponseEntity.ok(statusDTO);
    }

    @GetMapping("/all")
    public ResponseEntity<List<NotificationDetailsResponse>> getAllNotifications() {
        String userId = getUserIdByToken();
        return ResponseEntity.ok(notificationService.getAllNotifications(UUID.fromString(userId)));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationDetailsResponse> getNotificationById(
            @PathVariable UUID notificationId,
            @RequestHeader(value = "X-Request-Path", required = false, defaultValue = "") String requestPath) {
        
        // Verifica se o caminho está se tornando muito longo (possível recursão)
        if (requestPath.length() > MAX_PATH_LENGTH) {
            throw new InvalidPathException("Caminho da requisição muito longo. Possível recursão detectada.");
        }
        
        String userId = getUserIdByToken();
        NotificationDetailsResponse response = notificationService.getNotificationById(UUID.fromString(userId), notificationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam(required = false) String token) {
        String userId;
        
        // Tenta obter o userId do token da query primeiro
        if (token != null && !token.isEmpty()) {
            userId = tokenService.getUserIdFromToken(token);
        } else {
            // Se não houver token na query, tenta obter do header
            userId = tokenService.getCurrentUserId();
        }
        
        if (userId == null) {
            throw new RuntimeException("Token inválido ou ausente");
        }
        
        SseEmitter emitter = sseService.createEmitter(UUID.fromString(userId));
        
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Conexão SSE estabelecida com sucesso!")
                    .reconnectTime(10000));
        } catch (IOException e) {
            logger.error("Erro ao enviar evento de conexão SSE", e);
            throw new RuntimeException("Erro ao estabelecer conexão SSE", e);
        }
        
        return emitter;
    }

    private String getUserIdByToken() {
        String userId = tokenService.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("Token inválido ou ausente");
        }
        return userId;
    }
}
