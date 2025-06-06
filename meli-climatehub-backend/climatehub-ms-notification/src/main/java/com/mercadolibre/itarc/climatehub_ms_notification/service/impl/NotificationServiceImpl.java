package com.mercadolibre.itarc.climatehub_ms_notification.service.impl;

import com.mercadolibre.itarc.climatehub_ms_notification.constants.NotificationStatus;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.CityRequestDTO;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationStatusDTO;
import com.mercadolibre.itarc.climatehub_ms_notification.model.entity.NotificationEntity;
import com.mercadolibre.itarc.climatehub_ms_notification.model.mapper.NotificationMapper;
import com.mercadolibre.itarc.climatehub_ms_notification.producer.NotificationProducer;
import com.mercadolibre.itarc.climatehub_ms_notification.repository.NotificationRepository;
import com.mercadolibre.itarc.climatehub_ms_notification.service.NotificationService;
import com.mercadolibre.itarc.climatehub_ms_notification.service.SseService;
import com.mercadolibre.itarc.climatehub_ms_notification.service.TokenService;
import com.mercadolibre.itarc.climatehub_ms_notification.util.TimeUtils;
import com.mercadolibre.itarc.climatehub_ms_notification.util.TimeUtils.TimeAdjusted;
import com.mercadolibre.itarc.climatehub_ms_notification.validator.notification.NotificationValidator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationProducer notificationProducer;
    private final TokenService tokenService;
    private final SseService sseService;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            NotificationProducer notificationProducer,
            TokenService tokenService,
            SseService sseService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.notificationProducer = notificationProducer;
        this.tokenService = tokenService;
        this.sseService = sseService;
    }

    @Override
    public NotificationResponse scheduleNotification(NotificationRequest request) {
        NotificationValidator.validateNotificationRequest(request);
        LocalDateTime nextExecution = calculateNextExecution(request);

        NotificationEntity notification = notificationMapper.toEntity(request);
        // id da cidade irá ser buscado pelo worker.
        // o worker irá atualizar esse dado da notificação.
        notification.setCityId(0); 

        String userId = tokenService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in token");
        }
        notification.setUserId(UUID.fromString(userId));
        notification.setStatus(NotificationStatus.PENDING);
        notification.setNextExecution(nextExecution);

        // será trabalhado melhor em versões futuras.
        // por enquanto não esta sendo usado.
        notification.setCronExpression(createCronExpression(request));

        NotificationEntity scheduled = notificationRepository.save(notification);

        CityRequestDTO cityRequest = new CityRequestDTO(
                request.cityName(),
                request.uf(),
                request.scheduleType(),
                request.time(),
                request.executeAt(),
                request.dayOfWeek() != null ? DayOfWeek.of(request.dayOfWeek()) : null,
                request.endDate(),
                scheduled.getNotificationId(),
                scheduled.getUserId()
        );

        // envia para os workers processarem.
        notificationProducer.sendToProcess(cityRequest);

        return notificationMapper.toResponse(scheduled);
    }

    @Override
    public void updateStatus(UUID notificationId, NotificationStatusDTO statusDTO) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        notification.setStatus(statusDTO.getStatus());
        notificationRepository.save(notification);

        // Se houver um emitter SSE ativo para o usuário, envia a atualização
        if (sseService.hasEmitter(notification.getUserId())) {
            sseService.sendNotification(notification.getUserId(), statusDTO);
        }
    }

    private LocalDateTime calculateNextExecution(NotificationRequest request) {
        return switch (request.scheduleType()) {
            case ONCE -> request.executeAt();
            case DAILY -> {
                TimeAdjusted timeAdjusted = TimeUtils.adjustTime(request);
                LocalDateTime baseTime = timeAdjusted.getBaseTime();
                
                // Se o horário já passou hoje, agenda para amanhã
                if (baseTime.isBefore(timeAdjusted.getZonedNow().toLocalDateTime())) {
                    baseTime = baseTime.plusDays(1);
                    System.out.println("Ajustado para amanhã: " + baseTime);
                }
                
                // Converte de volta para UTC
                ZonedDateTime zonedProposed = baseTime.atZone(timeAdjusted.getZoneId());
                ZonedDateTime utcProposed = zonedProposed.withZoneSameInstant(ZoneId.of("UTC"));
                
                yield utcProposed.toLocalDateTime();
            }
            case WEEKLY -> {
                TimeAdjusted timeAdjusted = TimeUtils.adjustTime(request);
                LocalDateTime baseTime = timeAdjusted.getBaseTime();

                // Ajusta para o próximo dia da semana desejado
                while (baseTime.getDayOfWeek().getValue() != request.dayOfWeek()) {
                    baseTime = baseTime.plusDays(1);
                }
                
                // Se mesmo após ajustar o dia da semana, o horário já passou
                if (baseTime.isBefore(timeAdjusted.getZonedNow().toLocalDateTime())) {
                    baseTime = baseTime.plusDays(7);
                }
                
                // Converte de volta para UTC
                ZonedDateTime zonedBase = baseTime.atZone(timeAdjusted.getZoneId());
                ZonedDateTime utcBase = zonedBase.withZoneSameInstant(ZoneId.of("UTC"));
                
                yield utcBase.toLocalDateTime();
            }
            case CUSTOM -> throw new IllegalArgumentException("Tipo de agendamento CUSTOM não é suportado");
        };
    }

    private String createCronExpression(NotificationRequest request) {
        return switch (request.scheduleType()) {
            case DAILY -> createDailyCron(request.time());
            case WEEKLY -> createWeeklyCron(request.time(), request.dayOfWeek());
            case ONCE -> null;
            case CUSTOM -> throw new IllegalArgumentException("Tipo de agendamento CUSTOM não é suportado");
        };
    }
    
    private String createDailyCron(String time) {
        String[] parts = time.split(":");
        return String.format("0 %s %s * * ?", parts[1], parts[0]);
    }
    
    private String createWeeklyCron(String time, Integer dayOfWeek) {
        String[] parts = time.split(":");
        String[] days = {"", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        return String.format("0 %s %s ? * %s", parts[1], parts[0], days[dayOfWeek]);
    }
}
