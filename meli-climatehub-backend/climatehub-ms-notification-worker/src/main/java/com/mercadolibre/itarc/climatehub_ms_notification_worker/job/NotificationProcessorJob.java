package com.mercadolibre.itarc.climatehub_ms_notification_worker.job;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.NotificationStatus;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleStatus;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleType;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.feign.client.NotificationFeignClient;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.dto.NotificationStatusDTO;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.dto.WeatherNotificationDTO;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.entity.ScheduleEntity;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.PrevisaoCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.producer.NotificationSSEProducer;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.repository.ScheduleRepository;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.service.CptecService;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.service.impl.RedisOptOutService;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.util.TokenEncryptionUtil;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.service.TokenRefreshService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class NotificationProcessorJob implements Job {

    private static final String LOCK_KEY_PREFIX = "notification:lock:";
    private static final String PROCESSED_KEY_PREFIX = "notification:processed:";
    private static final long LOCK_DURATION = 5; // minutos
    private static final long PROCESSED_DURATION = 60; // 1 hora
    private static final long TOLERANCE_SECONDS = 2; // reduzido para 2 segundos
    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");


    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private CptecService cptecService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisOptOutService redisOptOutService;

    @Autowired
    private NotificationFeignClient notificationClient;

    @Autowired
    private NotificationSSEProducer notificationSSEProducer;

    @Autowired
    private TokenEncryptionUtil tokenEncryptionUtil;

    @Autowired
    private TokenRefreshService tokenRefreshService;

    @Override
    public void execute(JobExecutionContext context) {
        // Obtém o horário atual em UTC
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        ZonedDateTime spNow = utcNow.atZone(ZoneOffset.UTC).withZoneSameInstant(SAO_PAULO_ZONE);
        
        if (log.isDebugEnabled()) {
            log.debug("🕒 Iniciando processamento de notificações agendadas em: {} (UTC) / {} (SP)", 
                utcNow, spNow);
        }
        
        // Define o intervalo de busca com tolerância
        LocalDateTime startTime = utcNow.minus(TOLERANCE_SECONDS, ChronoUnit.SECONDS);
        LocalDateTime endTime = utcNow.plus(TOLERANCE_SECONDS, ChronoUnit.SECONDS);
        
        ZonedDateTime spStartTime = startTime.atZone(ZoneOffset.UTC).withZoneSameInstant(SAO_PAULO_ZONE);
        ZonedDateTime spEndTime = endTime.atZone(ZoneOffset.UTC).withZoneSameInstant(SAO_PAULO_ZONE);
        
        if (log.isDebugEnabled()) {
            log.debug("🔍 Buscando notificações entre {} e {} (UTC)", startTime, endTime);
            log.debug("🔍 Equivalente a {} e {} (SP)", spStartTime, spEndTime);
        }

        // Busca notificações no intervalo com tolerância
        List<ScheduleEntity> pendingNotifications = scheduleRepository
            .findPendingNotificationsWithTolerance(startTime, endTime, utcNow);
        
        if (log.isDebugEnabled()) {
            log.debug("📋 Encontradas {} notificações pendentes", pendingNotifications.size());
        }
        
        for (ScheduleEntity schedule : pendingNotifications) {
            String lockKey = LOCK_KEY_PREFIX + schedule.getId();
            String processedKey = PROCESSED_KEY_PREFIX + schedule.getId() + ":" + 
                utcNow.truncatedTo(ChronoUnit.HOURS).toString();
            
            // Verifica se já foi processado nesta hora
            if (Boolean.TRUE.equals(redisTemplate.hasKey(processedKey))) {
                if (log.isDebugEnabled()) {
                    log.debug("⏭️ Notificação {} já foi processada nesta hora", schedule.getId());
                }
                continue;
            }
            
            if (log.isDebugEnabled()) {
                log.debug("🔒 Tentando obter lock para notificação {} ({})", schedule.getId(), schedule.getNextExecution());
            }

            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "locked", LOCK_DURATION, TimeUnit.MINUTES);

            if (Boolean.TRUE.equals(acquired)) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("✅ Lock obtido para notificação {}", schedule.getId());
                    }
                    if(!redisOptOutService.isOptOut(schedule.getUserId())) {
                        processNotification(schedule);
                        // Marca como processado nesta hora
                        redisTemplate.opsForValue()
                            .set(processedKey, "processed", PROCESSED_DURATION, TimeUnit.SECONDS);
                    }
                } catch (Exception e) {
                    log.error("❌ Erro ao processar notificação {}: {}", schedule.getId(), e.getMessage(), e);
                    handleError(schedule);
                } finally {
                    redisTemplate.delete(lockKey);
                    if (log.isDebugEnabled()) {
                        log.debug("🔓 Lock liberado para notificação {}", schedule.getId());
                    }
                }
            } else if (log.isDebugEnabled()) {
                log.debug("⏭️ Notificação {} já está sendo processada por outra instância", schedule.getId());
            }
        }
    }

    private void processNotification(ScheduleEntity schedule) {
        // Obtém o horário atual em UTC
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        
        // Converte o horário agendado para UTC
        ZonedDateTime nextExecUtc = schedule.getNextExecution().atZone(ZoneOffset.UTC);
        ZonedDateTime nextExecSp = nextExecUtc.withZoneSameInstant(SAO_PAULO_ZONE);
        
        // Para agendamentos do tipo ONCE, verifica se está dentro da tolerância
        if (schedule.getScheduleType() == ScheduleType.ONCE) {
            long diffSeconds = Math.abs(ChronoUnit.SECONDS.between(utcNow, schedule.getNextExecution()));
            if (diffSeconds > TOLERANCE_SECONDS) {
                if (log.isDebugEnabled()) {
                    log.debug("⏳ Ainda não chegou o horário de execução para a notificação ONCE: {} (Agendada para: {} UTC / {} SP, Diferença: {} segundos)", 
                        schedule.getId(),
                        nextExecUtc,
                        nextExecSp,
                        diffSeconds);
                }
                return;
            }
        } else {
            // Para outros tipos de agendamento, mantém a verificação original
            if (utcNow.isBefore(schedule.getNextExecution()) && 
                ChronoUnit.SECONDS.between(utcNow, schedule.getNextExecution()) > 1) {
                if (log.isDebugEnabled()) {
                    log.debug("⏳ Ainda não chegou o horário de execução para a notificação: {} (Agendada para: {} UTC / {} SP)", 
                        schedule.getId(),
                        nextExecUtc,
                        nextExecSp);
                }
                return;
            }
        }

        try {
            // Busca previsão do tempo
            PrevisaoCache previsao = cptecService.getPrevisao(Integer.valueOf(schedule.getCityId()));

            if (previsao.getPrevisoes() == null || previsao.getPrevisoes().isEmpty()) {
                throw new RuntimeException("Nenhuma previsão encontrada para a cidade " + schedule.getCityName());
            }

            PrevisaoCache.PrevisaoDia previsaoHoje = previsao.getPrevisoes().get(0);

            // Log principal da previsão do tempo
            log.info("🌤️ Previsão do tempo para {} - {}: {}", 
                    schedule.getCityName(), 
                    schedule.getUf(),
                    previsaoHoje);

            // Envia a notificação para a fila SSE
            WeatherNotificationDTO weatherNotification = WeatherNotificationDTO.builder()
                    .userId(schedule.getUserId())
                    .notificationId(schedule.getNotificationId())
                    .cityName(schedule.getCityName())
                    .uf(schedule.getUf())
                    .date(LocalDate.parse(previsaoHoje.getDia()))
                    .minTemp(previsaoHoje.getMinima())
                    .maxTemp(previsaoHoje.getMaxima())
                    .message(String.format(
                            "Previsão do tempo para %s - %s em %s: Temperatura mínima: %.1f°C, Temperatura máxima: %.1f°C",
                            schedule.getCityName(),
                            schedule.getUf(),
                            previsaoHoje.getDia(),
                            (double) previsaoHoje.getMinima(),
                            (double) previsaoHoje.getMaxima()))
                    .build();

            // Descriptografa o token antes de enviar
            String encryptedToken = schedule.getAuthToken();
            String decryptedToken = encryptedToken != null ? tokenEncryptionUtil.decrypt(encryptedToken) : null;
            
            // Verifica se precisa renovar o token
            if (decryptedToken != null && tokenRefreshService.isTokenExpired(decryptedToken)) {
                // Renova o token
                String newToken = tokenRefreshService.refreshToken(decryptedToken);
                // Criptografa o novo token
                String newEncryptedToken = tokenEncryptionUtil.encrypt(newToken);
                // Atualiza o token no banco de dados
                schedule.setAuthToken(newEncryptedToken);
                scheduleRepository.save(schedule);
                // Usa o novo token para enviar a notificação
                decryptedToken = newToken;
            }
            
            // Envia para fila SSE usando o token (renovado ou não)
            notificationSSEProducer.sendToProcess(weatherNotification, decryptedToken);
            
            // Atualiza o status da notificação
            NotificationStatusDTO statusDTO = NotificationStatusDTO.builder()
                    .status(NotificationStatus.EXECUTED)
                    .message("Notificação enviada com sucesso")
                    .build();

            notificationClient.updateStatus(schedule.getNotificationId(), statusDTO);

            // Para agendamentos do tipo ONCE, marca como completado após enviar a previsão
            if (schedule.getScheduleType() == ScheduleType.ONCE) {
                schedule.setStatus(ScheduleStatus.COMPLETED);
                if (log.isDebugEnabled()) {
                    log.debug("✨ Agendamento único completado após envio da previsão");
                }
                scheduleRepository.save(schedule);
                return;
            }

            // Atualiza próxima execução
            updateNextExecution(schedule);
        } catch (Exception e) {
            log.error("❌ Erro ao processar notificação: {}", e.getMessage());
            NotificationStatusDTO errorStatus = NotificationStatusDTO.builder()
                    .status(NotificationStatus.FAILED)
                    .message("Erro ao obter previsão do tempo: " + e.getMessage())
                    .build();
            notificationClient.updateStatus(schedule.getNotificationId(), errorStatus);
        }
    }

    private void updateNextExecution(ScheduleEntity schedule) {
        // Obtém o horário atual em UTC
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        ZonedDateTime spNow = utcNow.atZone(ZoneOffset.UTC).withZoneSameInstant(SAO_PAULO_ZONE);
        LocalDateTime nextExecution = null;

        if (log.isDebugEnabled()) {
            log.debug("🔄 Atualizando próxima execução para {} - {} (Tipo: {})", 
                schedule.getCityName(), 
                schedule.getUf(),
                schedule.getScheduleType());
        }

        switch (schedule.getScheduleType()) {
            case ONCE:
                // Não faz nada aqui, pois já foi tratado no processNotification
                break;

            case DAILY:
                // Converte o horário agendado para UTC
                LocalDateTime spTime = LocalDateTime.of(spNow.toLocalDate(), schedule.getScheduleTime());
                ZonedDateTime spZoned = spTime.atZone(SAO_PAULO_ZONE);
                
                // Garante que a próxima execução será sempre no futuro
                while (spZoned.toLocalDateTime().isBefore(spNow.toLocalDateTime()) || 
                       spZoned.toLocalDateTime().equals(spNow.toLocalDateTime())) {
                    spZoned = spZoned.plusDays(1);
                }
                
                // Converte para UTC
                nextExecution = spZoned.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                if (log.isDebugEnabled()) {
                    log.debug("📅 Próxima execução diária agendada para: {} (UTC) / {} (SP)", 
                        nextExecution,
                        nextExecution.atZone(ZoneOffset.UTC).withZoneSameInstant(SAO_PAULO_ZONE));
                }
                break;

            case WEEKLY:
                // Converte o horário agendado para UTC
                spTime = LocalDateTime.of(spNow.toLocalDate(), schedule.getScheduleTime());
                spZoned = spTime.atZone(SAO_PAULO_ZONE).with(schedule.getDayOfWeek());
                
                // Garante que a próxima execução será sempre no futuro
                while (spZoned.toLocalDateTime().isBefore(spNow.toLocalDateTime()) || 
                       spZoned.toLocalDateTime().equals(spNow.toLocalDateTime())) {
                    spZoned = spZoned.plusWeeks(1);
                }
                
                // Converte para UTC
                nextExecution = spZoned.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                if (log.isDebugEnabled()) {
                    log.debug("📅 Próxima execução semanal agendada para: {} (UTC) / {} (SP)", 
                        nextExecution,
                        nextExecution.atZone(ZoneOffset.UTC).withZoneSameInstant(SAO_PAULO_ZONE));
                }
                break;
            default:
                break;
        }

        if (schedule.getEndDate() != null && utcNow.isAfter(schedule.getEndDate())) {
            schedule.setStatus(ScheduleStatus.COMPLETED);
            if (log.isDebugEnabled()) {
                log.debug("🏁 Agendamento finalizado por atingir a data limite");
            }
        } else if (nextExecution != null) {
            schedule.setNextExecution(nextExecution);
            if (log.isDebugEnabled()) {
                log.debug("⏰ Próxima execução atualizada para: {} (UTC) / {} (SP)", 
                    nextExecution,
                    nextExecution.atZone(ZoneOffset.UTC).withZoneSameInstant(SAO_PAULO_ZONE));
            }
        }

        scheduleRepository.save(schedule);
    }

    private void handleError(ScheduleEntity schedule) {
        schedule.setStatus(ScheduleStatus.ERROR);
        scheduleRepository.save(schedule);
    }
} 