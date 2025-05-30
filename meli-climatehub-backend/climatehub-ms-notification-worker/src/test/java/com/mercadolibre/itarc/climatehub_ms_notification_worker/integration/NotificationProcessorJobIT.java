package com.mercadolibre.itarc.climatehub_ms_notification_worker.integration;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleStatus;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleType;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.job.NotificationProcessorJob;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.entity.ScheduleEntity;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.PrevisaoCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.repository.ScheduleRepository;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.service.CptecService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.*;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
public class NotificationProcessorJobIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.0")
            .withExposedPorts(6379);

    @Autowired
    private NotificationProcessorJob notificationProcessorJob;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @MockBean
    private CptecService cptecService;

    @MockBean
    private JobExecutionContext jobExecutionContext;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @BeforeEach
    void setUp() {
        scheduleRepository.deleteAll();
    }

    @Test
    void shouldProcessDailyScheduleSuccessfully() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        ScheduleEntity schedule = new ScheduleEntity();
        schedule.setNotificationId(notificationId);
        schedule.setUserId(userId);
        schedule.setCityId("123");
        schedule.setCityName("São Paulo");
        schedule.setUf("SP");
        schedule.setScheduleType(ScheduleType.DAILY);
        schedule.setScheduleTime(LocalTime.of(8, 0));
        schedule.setNextExecution(LocalDateTime.now().minusMinutes(1));
        schedule.setStatus(ScheduleStatus.ACTIVE);

        scheduleRepository.save(schedule);

        PrevisaoCache.PrevisaoDia previsaoDia = new PrevisaoCache.PrevisaoDia();
        previsaoDia.setDia("2025-06-04");
        previsaoDia.setTempo("pn");
        previsaoDia.setMaxima(28);
        previsaoDia.setMinima(18);
        previsaoDia.setIuv(8.0);

        PrevisaoCache previsao = new PrevisaoCache();
        previsao.setNome("São Paulo");
        previsao.setUf("SP");
        previsao.setAtualizacao("2025-06-04");
        previsao.setPrevisoes(Collections.singletonList(previsaoDia));

        when(cptecService.getPrevisao(any())).thenReturn(previsao);

        // When
        notificationProcessorJob.execute(jobExecutionContext);

        // Then
        ScheduleEntity updatedSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertNotNull(updatedSchedule);
        assertEquals(ScheduleStatus.ACTIVE, updatedSchedule.getStatus());

        // Verifica se a próxima execução foi agendada para amanhã no mesmo horário
        ZonedDateTime expectedNext = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"))
                .toLocalDate()
                .atTime(LocalTime.of(8, 0))
                .atZone(ZoneId.of("America/Sao_Paulo"));

        if (expectedNext.isBefore(ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")))) {
            expectedNext = expectedNext.plusDays(1);
        }

        assertEquals(
            expectedNext.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
            updatedSchedule.getNextExecution()
        );
    }

    @Test
    void shouldProcessWeeklyScheduleSuccessfully() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        ScheduleEntity schedule = new ScheduleEntity();
        schedule.setNotificationId(notificationId);
        schedule.setUserId(userId);
        schedule.setCityId("123");
        schedule.setCityName("São Paulo");
        schedule.setUf("SP");
        schedule.setScheduleType(ScheduleType.WEEKLY);
        schedule.setScheduleTime(LocalTime.of(8, 0));
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setNextExecution(LocalDateTime.now().minusMinutes(1));
        schedule.setStatus(ScheduleStatus.ACTIVE);

        scheduleRepository.save(schedule);

        PrevisaoCache.PrevisaoDia previsaoDia = new PrevisaoCache.PrevisaoDia();
        previsaoDia.setDia("2025-06-04");
        previsaoDia.setTempo("pn");
        previsaoDia.setMaxima(28);
        previsaoDia.setMinima(18);
        previsaoDia.setIuv(8.0);

        PrevisaoCache previsao = new PrevisaoCache();
        previsao.setNome("São Paulo");
        previsao.setUf("SP");
        previsao.setAtualizacao("2025-06-04");
        previsao.setPrevisoes(Collections.singletonList(previsaoDia));

        when(cptecService.getPrevisao(any())).thenReturn(previsao);

        // When
        notificationProcessorJob.execute(jobExecutionContext);

        // Then
        ScheduleEntity updatedSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertNotNull(updatedSchedule);
        assertEquals(ScheduleStatus.ACTIVE, updatedSchedule.getStatus());

        // Verifica se a próxima execução foi agendada para a próxima segunda-feira
        ZonedDateTime expectedNext = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"))
                .toLocalDate()
                .atTime(LocalTime.of(8, 0))
                .atZone(ZoneId.of("America/Sao_Paulo"))
                .with(DayOfWeek.MONDAY);

        if (expectedNext.isBefore(ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")))) {
            expectedNext = expectedNext.plusWeeks(1);
        }

        assertEquals(
            expectedNext.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
            updatedSchedule.getNextExecution()
        );
    }

    @Test
    void shouldProcessOneTimeScheduleSuccessfully() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        ScheduleEntity schedule = new ScheduleEntity();
        schedule.setNotificationId(notificationId);
        schedule.setUserId(userId);
        schedule.setCityId("123");
        schedule.setCityName("São Paulo");
        schedule.setUf("SP");
        schedule.setScheduleType(ScheduleType.ONCE);
        schedule.setNextExecution(LocalDateTime.now().minusMinutes(1));
        schedule.setStatus(ScheduleStatus.ACTIVE);

        scheduleRepository.save(schedule);

        PrevisaoCache.PrevisaoDia previsaoDia = new PrevisaoCache.PrevisaoDia();
        previsaoDia.setDia("2025-06-04");
        previsaoDia.setTempo("pn");
        previsaoDia.setMaxima(28);
        previsaoDia.setMinima(18);
        previsaoDia.setIuv(8.0);

        PrevisaoCache previsao = new PrevisaoCache();
        previsao.setNome("São Paulo");
        previsao.setUf("SP");
        previsao.setAtualizacao("2025-06-04");
        previsao.setPrevisoes(Collections.singletonList(previsaoDia));

        when(cptecService.getPrevisao(any())).thenReturn(previsao);

        // When
        notificationProcessorJob.execute(jobExecutionContext);

        // Then
        ScheduleEntity updatedSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertNotNull(updatedSchedule);
        assertEquals(ScheduleStatus.COMPLETED, updatedSchedule.getStatus());
    }

    @Test
    void shouldHandleEndDateReached() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        ScheduleEntity schedule = new ScheduleEntity();
        schedule.setNotificationId(notificationId);
        schedule.setUserId(userId);
        schedule.setCityId("123");
        schedule.setCityName("São Paulo");
        schedule.setUf("SP");
        schedule.setScheduleType(ScheduleType.DAILY);
        schedule.setScheduleTime(LocalTime.of(8, 0));
        schedule.setNextExecution(LocalDateTime.now().minusMinutes(1));
        schedule.setEndDate(LocalDateTime.now().minusDays(1));
        schedule.setStatus(ScheduleStatus.ACTIVE);

        scheduleRepository.save(schedule);

        // When
        notificationProcessorJob.execute(jobExecutionContext);

        // Then
        ScheduleEntity updatedSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertNotNull(updatedSchedule);
        assertEquals(ScheduleStatus.COMPLETED, updatedSchedule.getStatus());
    }
} 