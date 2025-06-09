package com.mercadolibre.itarc.climatehub_ms_notification.service.impl;

import com.mercadolibre.itarc.climatehub_ms_notification.constants.NotificationStatus;
import com.mercadolibre.itarc.climatehub_ms_notification.exception.BusinessException;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.*;
import com.mercadolibre.itarc.climatehub_ms_notification.model.entity.NotificationEntity;
import com.mercadolibre.itarc.climatehub_ms_notification.model.mapper.NotificationMapper;
import com.mercadolibre.itarc.climatehub_ms_notification.model.redis.CityCache;
import com.mercadolibre.itarc.climatehub_ms_notification.producer.NotificationProducer;
import com.mercadolibre.itarc.climatehub_ms_notification.repository.NotificationRepository;
import com.mercadolibre.itarc.climatehub_ms_notification.service.CptecService;
import com.mercadolibre.itarc.climatehub_ms_notification.service.NotificationService;
import com.mercadolibre.itarc.climatehub_ms_notification.service.SseService;
import com.mercadolibre.itarc.climatehub_ms_notification.service.TokenService;
import com.mercadolibre.itarc.climatehub_ms_notification.util.TimeUtils;
import com.mercadolibre.itarc.climatehub_ms_notification.util.TimeUtils.TimeAdjusted;
import com.mercadolibre.itarc.climatehub_ms_notification.validator.notification.NotificationValidator;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationProducer notificationProducer;
    private final TokenService tokenService;
    private final SseService sseService;
    private final CptecService cptecService;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            NotificationProducer notificationProducer,
            TokenService tokenService,
            SseService sseService,
            CptecService cptecService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.notificationProducer = notificationProducer;
        this.tokenService = tokenService;
        this.sseService = sseService;
        this.cptecService = cptecService;
    }

    private static final int MAX_NOTIFICATION_DEPTH = 5; // Profundidade máxima de notificações encadeadas
    
    @Override
    @Transactional
    public NotificationResponse scheduleNotification(NotificationRequest request) {
        logger.info("Service - Iniciando validação da requisição: {}", request);
        NotificationValidator.validateNotificationRequest(request);
        logger.info("Service - Validação concluída com sucesso");

        logger.info("Service - Buscando informações da cidade: {} - {}", request.cityName(), request.uf());
        CityCache cityInfo = cptecService.getCityId(request.cityName(), request.uf());
        if (cityInfo == null) {
            logger.error("Service - Cidade não encontrada: {} - {}", request.cityName(), request.uf());
            throw new BusinessException("Cidade não encontrada");
        }
        logger.info("Service - Informações da cidade encontradas: {}", cityInfo);

        logger.info("Service - Calculando próxima execução");
        LocalDateTime nextExecution = calculateNextExecution(request);
        logger.info("Service - Próxima execução calculada: {}", nextExecution);

        logger.info("Service - Criando entidade de notificação");
        NotificationEntity notification = notificationMapper.toEntity(request);
        notification.setCityId(cityInfo.getCityId());

        String userId = tokenService.getCurrentUserId();
        if (userId == null) {
            logger.error("Service - ID do usuário não encontrado no token");
            throw new IllegalStateException("User ID not found in token");
        }
        logger.info("Service - ID do usuário obtido: {}", userId);

        notification.setUserId(UUID.fromString(userId));
        notification.setStatus(NotificationStatus.PENDING);
        notification.setNextExecution(nextExecution);
        notification.setCronExpression(createCronExpression(request));

        logger.info("Service - Salvando notificação no banco de dados");
        NotificationEntity scheduled = notificationRepository.save(notification);
        logger.info("Service - Notificação salva com sucesso: {}", scheduled);

        CityRequestDTO cityRequest = new CityRequestDTO(
                request.cityName(),
                request.uf(),
                request.scheduleType(),
                request.time(),
                request.executeAt(),
                request.dayOfWeek() != null ? DayOfWeek.of(request.dayOfWeek()) : null,
                request.endDate(),
                scheduled.getNotificationId(),
                scheduled.getUserId(),
                cityInfo
        );

        logger.info("Service - Enviando requisição para processamento: {}", cityRequest);
        notificationProducer.sendToProcess(cityRequest);
        logger.info("Service - Requisição enviada com sucesso");

        NotificationResponse response = notificationMapper.toResponse(scheduled);
        logger.info("Service - Retornando resposta: {}", response);
        return response;
    }

    @Override
    public List<NotificationDetailsResponse> getAllNotifications(UUID userId) {
        return notificationRepository.getAllByUserId(userId)
                .stream()
                .map(notificationMapper::toDetails)
                .toList();
    }

    @Override
    @Transactional
    public NotificationDetailsResponse getNotificationById(UUID userId, UUID notificationId) {
        // Validação adicional para prevenir referências circulares
        if (notificationId == null) {
            throw new IllegalArgumentException("ID da notificação não pode ser nulo");
        }

        return notificationRepository.findByNotificationIdAndUserId(notificationId, userId)
                .map(notificationMapper::toDetails)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));
    }

    @Override
    public void updateStatus(UUID notificationId, NotificationStatusDTO statusDTO) {
        logger.debug("Atualizando status da notificação {} para {}", notificationId, statusDTO.getStatus());

        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));

        // Valida a transição de status
        if (!isValidStatusTransition(notification.getStatus(), statusDTO.getStatus())) {
            throw new IllegalStateException("Transição de status inválida: " + notification.getStatus() + " -> " + statusDTO.getStatus());
        }

        try {
            notification.setStatus(statusDTO.getStatus());
            notificationRepository.save(notification);
            logger.debug("Status da notificação {} atualizado com sucesso para {}", notificationId, statusDTO.getStatus());
        } catch (Exception e) {
            logger.error("Erro ao atualizar status da notificação {}", notificationId, e);
            throw new RuntimeException("Erro ao atualizar status da notificação", e);
        }
    }

    private boolean isValidStatusTransition(NotificationStatus currentStatus, NotificationStatus newStatus) {
        if (currentStatus == newStatus) {
            return false; // Evita atualizações desnecessárias
        }

        return switch (currentStatus) {
            case PENDING -> true; // PENDING pode ir para qualquer status
            case EXECUTED -> newStatus == NotificationStatus.FAILED; // EXECUTED só pode ir para FAILED
            case FAILED -> false; // FAILED é um estado terminal
        };
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
                    baseTime = baseTime.plusWeeks(1);
                }
                
                // Converte de volta para UTC
                ZonedDateTime zonedProposed = baseTime.atZone(timeAdjusted.getZoneId());
                ZonedDateTime utcProposed = zonedProposed.withZoneSameInstant(ZoneId.of("UTC"));
                
                yield utcProposed.toLocalDateTime();
            }
            case CUSTOM -> throw new IllegalArgumentException("CUSTOM is not supported in this version.");
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
