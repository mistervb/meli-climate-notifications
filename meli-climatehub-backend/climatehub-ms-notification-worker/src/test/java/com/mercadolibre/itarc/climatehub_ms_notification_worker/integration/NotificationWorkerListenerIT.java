package com.mercadolibre.itarc.climatehub_ms_notification_worker.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.config.RabbitMQConfig;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleStatus;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.constants.ScheduleType;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.dto.CityRequestDTO;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.entity.ScheduleEntity;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.CityCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.repository.ScheduleRepository;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.service.CptecService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
public class NotificationWorkerListenerIT {
/*
    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.11-management")
            .withExposedPorts(5672, 15672);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.0")
            .withExposedPorts(6379);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CptecService cptecService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getFirstMappedPort);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @BeforeEach
    void setUp() {
        scheduleRepository.deleteAll();
    }

    @Test
    void shouldCreateDailyScheduleSuccessfully() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        CityRequestDTO request = new CityRequestDTO();
        request.setNotificationId(notificationId);
        request.setUserId(userId);
        request.setCityName("São Paulo");
        request.setUf("SP");
        request.setScheduleType(ScheduleType.DAILY);
        request.setTime("08:00");

        CityCache cityCache = new CityCache();
        cityCache.setCityId(123);
        cityCache.setUf("SP");

        when(cptecService.getCityId(request.getCityName(), request.getUf()))
                .thenReturn(cityCache);

        // When
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, request);
        Thread.sleep(2000); // Aguarda o processamento

        // Then
        ScheduleEntity schedule = scheduleRepository.findAll().get(0);
        assertNotNull(schedule);
        assertEquals(request.getNotificationId(), schedule.getNotificationId());
        assertEquals(request.getUserId(), schedule.getUserId());
        assertEquals(request.getCityName(), schedule.getCityName());
        assertEquals(request.getUf(), schedule.getUf());
        assertEquals(ScheduleStatus.ACTIVE, schedule.getStatus());
        assertEquals(ScheduleType.DAILY, schedule.getScheduleType());

        // Verifica se a próxima execução está correta
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        LocalTime scheduleTime = LocalTime.parse(request.getTime());
        ZonedDateTime expectedNext = now.toLocalDate()
                .atTime(scheduleTime)
                .atZone(ZoneId.of("America/Sao_Paulo"));
        
        if (expectedNext.isBefore(now)) {
            expectedNext = expectedNext.plusDays(1);
        }

        assertEquals(
            expectedNext.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
            schedule.getNextExecution()
        );
    }

    @Test
    void shouldCreateWeeklyScheduleSuccessfully() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        CityRequestDTO request = new CityRequestDTO();
        request.setNotificationId(notificationId);
        request.setUserId(userId);
        request.setCityName("São Paulo");
        request.setUf("SP");
        request.setScheduleType(ScheduleType.WEEKLY);
        request.setTime("08:00");
        request.setDayOfWeek(DayOfWeek.MONDAY);

        CityCache cityCache = new CityCache();
        cityCache.setCityId(123);
        cityCache.setUf("SP");

        when(cptecService.getCityId(request.getCityName(), request.getUf()))
                .thenReturn(cityCache);

        // When
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, request);
        Thread.sleep(2000); // Aguarda o processamento

        // Then
        ScheduleEntity schedule = scheduleRepository.findAll().get(0);
        assertNotNull(schedule);
        assertEquals(request.getNotificationId(), schedule.getNotificationId());
        assertEquals(request.getUserId(), schedule.getUserId());
        assertEquals(request.getCityName(), schedule.getCityName());
        assertEquals(request.getUf(), schedule.getUf());
        assertEquals(ScheduleStatus.ACTIVE, schedule.getStatus());
        assertEquals(ScheduleType.WEEKLY, schedule.getScheduleType());
        assertEquals(request.getDayOfWeek(), schedule.getDayOfWeek());

        // Verifica se a próxima execução está na próxima segunda-feira
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        LocalTime scheduleTime = LocalTime.parse(request.getTime());
        ZonedDateTime expectedNext = now.toLocalDate()
                .atTime(scheduleTime)
                .atZone(ZoneId.of("America/Sao_Paulo"))
                .with(DayOfWeek.MONDAY);
        
        if (expectedNext.isBefore(now)) {
            expectedNext = expectedNext.plusWeeks(1);
        }

        assertEquals(
            expectedNext.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
            schedule.getNextExecution()
        );
    }

    @Test
    void shouldCreateOneTimeScheduleSuccessfully() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime executeAt = LocalDateTime.now().plusHours(1);
        
        CityRequestDTO request = new CityRequestDTO();
        request.setNotificationId(notificationId);
        request.setUserId(userId);
        request.setCityName("São Paulo");
        request.setUf("SP");
        request.setScheduleType(ScheduleType.ONCE);
        request.setExecuteAt(executeAt);

        CityCache cityCache = new CityCache();
        cityCache.setCityId(123);
        cityCache.setUf("SP");

        when(cptecService.getCityId(request.getCityName(), request.getUf()))
                .thenReturn(cityCache);

        // When
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, request);
        Thread.sleep(2000); // Aguarda o processamento

        // Then
        ScheduleEntity schedule = scheduleRepository.findAll().get(0);
        assertNotNull(schedule);
        assertEquals(request.getNotificationId(), schedule.getNotificationId());
        assertEquals(request.getUserId(), schedule.getUserId());
        assertEquals(request.getCityName(), schedule.getCityName());
        assertEquals(request.getUf(), schedule.getUf());
        assertEquals(ScheduleStatus.ACTIVE, schedule.getStatus());
        assertEquals(ScheduleType.ONCE, schedule.getScheduleType());
        assertEquals(executeAt, schedule.getNextExecution());
    }

    @Test
    void shouldHandleCityNotFound() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        CityRequestDTO request = new CityRequestDTO();
        request.setNotificationId(notificationId);
        request.setUserId(userId);
        request.setCityName("Cidade Inexistente");
        request.setUf("XX");
        request.setScheduleType(ScheduleType.DAILY);
        request.setTime("08:00");

        when(cptecService.getCityId(request.getCityName(), request.getUf()))
                .thenReturn(null);

        // When
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, request);
        Thread.sleep(2000); // Aguarda o processamento

        // Then
        assertTrue(scheduleRepository.findAll().isEmpty());
    }
 */
} 