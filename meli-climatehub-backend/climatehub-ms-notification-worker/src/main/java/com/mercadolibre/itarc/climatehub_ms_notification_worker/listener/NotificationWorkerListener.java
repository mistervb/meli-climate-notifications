package com.mercadolibre.itarc.climatehub_ms_notification_worker.listener;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.config.RabbitMQConfig;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.NotificationStatus;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleStatus;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.dto.CityRequestDTO;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.entity.ScheduleEntity;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.CityCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.repository.ScheduleRepository;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.service.CptecService;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.util.TokenEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
@Slf4j
public class NotificationWorkerListener {
    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");
    
    private final CptecService cptecService;
    private final ScheduleRepository scheduleRepository;
    private final TokenEncryptionUtil tokenEncryptionUtil;

    public NotificationWorkerListener(
            CptecService cptecService,
            ScheduleRepository scheduleRepository,
            TokenEncryptionUtil tokenEncryptionUtil
    ) {
        this.cptecService = cptecService;
        this.scheduleRepository = scheduleRepository;
        this.tokenEncryptionUtil = tokenEncryptionUtil;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void receive(CityRequestDTO request, Message message) {
        try {
            // Extrai o token JWT do header da mensagem
            String token = extractToken(message);
            log.debug("Recebido pedido de agendamento: {}", request);

            // Cria o agendamento com o token criptografado
            ScheduleEntity schedule = createSchedule(request, request.getCityInfo());
            if (token != null) {
                String encryptedToken = tokenEncryptionUtil.encrypt(token);
                schedule.setAuthToken(encryptedToken);
            }
            scheduleRepository.save(schedule);
        } catch (Exception e) {
            log.error("Erro ao processar mensagem: {}", e.getMessage(), e);
            throw new AmqpRejectAndDontRequeueException("Erro ao processar mensagem", e);
        }
    }

    private String extractToken(Message message) {
        MessageProperties properties = message.getMessageProperties();
        if (properties != null && properties.getHeaders().containsKey("Authorization")) {
            return properties.getHeaders().get("Authorization").toString();
        }
        return null;
    }

    private ScheduleEntity createSchedule(CityRequestDTO request, CityCache cityInfo) {
        LocalDateTime nextExecution;
        LocalTime scheduleTime = null;

        // Obtém o horário atual em UTC
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        ZonedDateTime spNow = utcNow.atZone(ZoneOffset.UTC).withZoneSameInstant(SAO_PAULO_ZONE);

        switch (request.getScheduleType()) {
            case ONCE:
                // Para agendamentos únicos, assume que o executeAt já está em UTC
                nextExecution = request.getExecuteAt();
                // Extrai o horário do executeAt mantendo em UTC
                scheduleTime = request.getExecuteAt().toLocalTime();
                break;
            case DAILY:
                scheduleTime = LocalTime.parse(request.getTime());
                // Cria o próximo horário de execução em São Paulo
                ZonedDateTime spNext = spNow.toLocalDate()
                    .atTime(scheduleTime)
                    .atZone(SAO_PAULO_ZONE);
                
                // Se o horário já passou hoje, agenda para amanhã
                if (spNext.toLocalDateTime().isBefore(spNow.toLocalDateTime())) {
                    spNext = spNext.plusDays(1);
                }
                
                // Converte para UTC
                nextExecution = spNext.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                break;
            case WEEKLY:
                scheduleTime = LocalTime.parse(request.getTime());
                // Cria o próximo horário de execução em São Paulo
                ZonedDateTime weeklyNext = spNow.toLocalDate()
                    .atTime(scheduleTime)
                    .atZone(SAO_PAULO_ZONE)
                    .with(request.getDayOfWeek());
                
                // Se o horário já passou esta semana, agenda para a próxima
                if (weeklyNext.toLocalDateTime().isBefore(spNow.toLocalDateTime())) {
                    weeklyNext = weeklyNext.plusWeeks(1);
                }
                
                // Converte para UTC
                nextExecution = weeklyNext.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                break;
            default:
                throw new IllegalArgumentException("Tipo de agendamento não suportado: " + request.getScheduleType());
        }

        return ScheduleEntity.builder()
                .notificationId(request.getNotificationId())
                .userId(request.getUserId())
                .cityId(String.valueOf(cityInfo.getCityId()))
                .cityName(request.getCityName())
                .uf(request.getUf())
                .scheduleType(request.getScheduleType())
                .scheduleTime(scheduleTime)
                .dayOfWeek(request.getDayOfWeek())
                .nextExecution(nextExecution)
                .endDate(request.getEndDate())
                .status(ScheduleStatus.ACTIVE)
                .build();
    }
}
