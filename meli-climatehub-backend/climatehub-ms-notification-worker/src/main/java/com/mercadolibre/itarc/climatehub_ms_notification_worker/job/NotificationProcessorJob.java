package com.mercadolibre.itarc.climatehub_ms_notification_worker.job;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.NotificationStatus;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleStatus;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleType;
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
    private static final long LOCK_DURATION = 2; // 2 minutos
    private static final long PROCESSED_DURATION = 60; // 1 hora
    private static final long TOLERANCE_SECONDS = 10; // 10 segundos
    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_RETRIES = 10;
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 segundo

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private CptecService cptecService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisOptOutService redisOptOutService;

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

        // Busca notificações no intervalo com tolerância
        List<ScheduleEntity> pendingNotifications = scheduleRepository
            .findPendingNotificationsWithTolerance(startTime, endTime, utcNow);

        if (log.isDebugEnabled()) {
            log.debug("📋 Encontradas {} notificações pendentes no intervalo {} a {}",
                pendingNotifications.size(), startTime, endTime);
        }

        // Processa as notificações em ordem de horário
        pendingNotifications.stream()
            .sorted((a, b) -> a.getNextExecution().compareTo(b.getNextExecution()))
            .forEach(schedule -> {
                String lockKey = LOCK_KEY_PREFIX + schedule.getId();
                String processedKey = PROCESSED_KEY_PREFIX + schedule.getId() + ":" +
                    utcNow.truncatedTo(ChronoUnit.HOURS).toString();

                // Verifica se já foi processado nesta hora
                if (Boolean.TRUE.equals(redisTemplate.hasKey(processedKey))) {
                    if (log.isDebugEnabled()) {
                        log.debug("⏭️ Agendamento {} já foi processado nesta hora", schedule.getId());
                    }
                    return;
                }

                // Verifica se já chegou o horário de execução
                LocalDateTime scheduledTime = schedule.getNextExecution();
                if (utcNow.isBefore(scheduledTime)) {
                    long secondsUntil = ChronoUnit.SECONDS.between(utcNow, scheduledTime);
                    if (secondsUntil > 1) { // Se falta mais de 1 segundo
                        if (log.isDebugEnabled()) {
                            log.debug("⏳ Ainda não chegou o horário de execução para o agendamento: {} (Agendado para: {} UTC / {} SP)",
                                schedule.getId(),
                                scheduledTime,
                                scheduledTime.atZone(ZoneOffset.UTC).withZoneSameInstant(SAO_PAULO_ZONE));
                        }
                        return;
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("🔒 Tentando obter lock para agendamento {} ({})",
                        schedule.getId(), schedule.getNextExecution());
                }

                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, "locked", LOCK_DURATION, TimeUnit.MINUTES);

                if (Boolean.TRUE.equals(acquired)) {
                    try {
                        if(!redisOptOutService.isOptOut(schedule.getUserId())) {
                            // Processa a notificação com retry e guarda o resultado
                            boolean success = false;
                            try {
                                processNotificationWithRetry(schedule);
                                success = true;
                            } catch (Exception e) {
                                log.error("❌ Erro ao processar agendamento {}: {}",
                                    schedule.getId(), e.getMessage(), e);
                                handleError(schedule);
                            }

                            // Marca como processado SOMENTE se processou com sucesso
                            if (success) {
                                redisTemplate.opsForValue()
                                    .set(processedKey, "processed", PROCESSED_DURATION, TimeUnit.SECONDS);
                                if (log.isDebugEnabled()) {
                                    log.debug("✅ Agendamento {} processado e marcado com sucesso", schedule.getId());
                                }
                            }
                        }
                    } finally {
                        redisTemplate.delete(lockKey);
                    }
                }
            });
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
                    log.debug("⏳ Ainda não chegou o horário de execução para o agendamento ONCE: {} (Agendado para: {} UTC / {} SP, Diferença: {} segundos)",
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
                    log.debug("⏳ Ainda não chegou o horário de execução para o agendamento: {} (Agendado para: {} UTC / {} SP)",
                        schedule.getId(),
                        nextExecUtc,
                        nextExecSp);
                }
                return;
            }
        }

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

        // Log successful processing
        log.info("Notificação processada com sucesso: {}", schedule.getNotificationId());

        // Verifica se já chegou o horário de execução
        LocalDateTime scheduledTime = schedule.getNextExecution();
        if (utcNow.isBefore(scheduledTime)) {
            long secondsUntil = ChronoUnit.SECONDS.between(utcNow, scheduledTime);
            if (secondsUntil > 1) { // Se falta mais de 1 segundo
                if (log.isDebugEnabled()) {
                    log.debug("Ainda não chegou o horário de execução para o agendamento: {} (Agendado para: {} UTC / {} SP)",
                        schedule.getId(),
                        scheduledTime,
                        scheduledTime.atZone(ZoneOffset.UTC).withZoneSameInstant(SAO_PAULO_ZONE));
                }
                return;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Tentando obter lock para agendamento {} ({})",
                schedule.getId(), schedule.getNextExecution());
        }

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

    private void processNotificationWithRetry(ScheduleEntity schedule) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    // Backoff exponencial: 1s, 2s, 4s
                    long delayMs = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
                    if (log.isDebugEnabled()) {
                        log.debug("🔄 Tentativa {} de {}, aguardando {}ms antes de tentar novamente...",
                            attempt + 1, MAX_RETRIES, delayMs);
                    }
                    Thread.sleep(delayMs);
                }

                processNotification(schedule);
                // Se chegou aqui, processou com sucesso
                return;

            } catch (Exception e) {
                lastException = e;
                if (log.isDebugEnabled()) {
                    log.debug("❌ Erro na tentativa {} de {}: {}",
                        attempt + 1, MAX_RETRIES, e.getMessage());
                }

                // Se não for erro de rede, não tenta novamente
                if (!isNetworkError(e)) {
                    if (log.isDebugEnabled()) {
                        log.debug("⚠️ Erro não é de rede, não tentará novamente");
                    }
                    throw e; // Propaga o erro imediatamente se não for de rede
                }
            }
        }

        // Se chegou aqui, todas as tentativas falharam
        log.error("❌ Erro ao processar notificação após {} tentativas: {}",
            MAX_RETRIES, lastException.getMessage(), lastException);
        throw lastException; // Propaga o último erro após todas as tentativas
    }

    private boolean isNetworkError(Exception e) {
        if (e instanceof java.net.ConnectException ||
            e instanceof java.net.SocketTimeoutException ||
            e instanceof java.net.UnknownHostException ||
            e instanceof org.springframework.web.client.ResourceAccessException) {
            return true;
        }

        // Verifica se a causa raiz é um erro de rede
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof java.net.ConnectException ||
                cause instanceof java.net.SocketTimeoutException ||
                cause instanceof java.net.UnknownHostException ||
                cause instanceof org.springframework.web.client.ResourceAccessException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
