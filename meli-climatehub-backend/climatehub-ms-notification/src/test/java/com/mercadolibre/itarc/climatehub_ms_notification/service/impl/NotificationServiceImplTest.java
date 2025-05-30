package com.mercadolibre.itarc.climatehub_ms_notification.service.impl;

import com.mercadolibre.itarc.climatehub_ms_notification.constants.NotificationStatus;
import com.mercadolibre.itarc.climatehub_ms_notification.constants.ScheduleType;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.CityRequestDTO;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationStatusDTO;
import com.mercadolibre.itarc.climatehub_ms_notification.model.entity.NotificationEntity;
import com.mercadolibre.itarc.climatehub_ms_notification.model.mapper.NotificationMapper;
import com.mercadolibre.itarc.climatehub_ms_notification.producer.NotificationProducer;
import com.mercadolibre.itarc.climatehub_ms_notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private NotificationRequest request;
    private NotificationEntity entity;
    private NotificationResponse response;
    private final UUID userId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        request = new NotificationRequest(
                "São Paulo",
                "SP",
                ScheduleType.DAILY,
                "10:00",
                null,
                null,
                LocalDateTime.now().plusDays(1)
        );

        entity = mock(NotificationEntity.class);
        when(entity.getNotificationId()).thenReturn(UUID.randomUUID());
        when(entity.getType()).thenReturn(ScheduleType.DAILY);
        when(entity.getTime()).thenReturn("10:00");
        when(entity.getStatus()).thenReturn(NotificationStatus.PENDING);
        when(entity.getNextExecution()).thenReturn(now.withHour(10).withMinute(0).withSecond(0));
        when(entity.getEndDate()).thenReturn(LocalDateTime.now().plusDays(1));
        when(entity.getCronExpression()).thenReturn("0 0 10 * * ?");
        when(entity.getUserId()).thenReturn(userId);
        when(entity.getCityId()).thenReturn(1);

        response = new NotificationResponse(
                entity.getNotificationId(),
                entity.getNextExecution()
        );
    }

    @Nested
    @DisplayName("Schedule Notification Tests")
    class ScheduleNotificationTests {
        
        @Test
        @DisplayName("Deve agendar notificação diária com sucesso")
        void success_scheduleNotification_daily() {
            when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(entity);
            when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(entity);
            when(notificationMapper.toResponse(any(NotificationEntity.class))).thenReturn(response);
            
            NotificationResponse result = notificationService.scheduleNotification(request);
            
            assertNotNull(result);
            assertEquals(entity.getNotificationId(), result.notificationId());
            assertEquals(entity.getNextExecution(), result.nextExecution());
            
            verify(notificationProducer).sendToProcess(any(CityRequestDTO.class));
            verify(notificationRepository).save(any(NotificationEntity.class));
        }

        @Test
        @DisplayName("Deve agendar notificação única com sucesso")
        void success_scheduleNotification_once() {
            request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.ONCE,
                    null,
                    null,
                    LocalDateTime.now().plusHours(1),
                    null
            );

            when(entity.getType()).thenReturn(ScheduleType.ONCE);
            when(entity.getTime()).thenReturn(null);
            when(entity.getNextExecution()).thenReturn(LocalDateTime.now().plusHours(1));
            when(entity.getCronExpression()).thenReturn(null);

            when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(entity);
            when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(entity);
            when(notificationMapper.toResponse(any(NotificationEntity.class))).thenReturn(response);

            NotificationResponse result = notificationService.scheduleNotification(request);

            assertNotNull(result);
            verify(notificationProducer).sendToProcess(any(CityRequestDTO.class));
            verify(notificationRepository).save(any(NotificationEntity.class));
        }

        @Test
        @DisplayName("Deve agendar notificação semanal com sucesso")
        void success_scheduleNotification_weekly() {
            request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.WEEKLY,
                    "10:00",
                    2,
                    null,
                    LocalDateTime.now().plusDays(7)
            );

            when(entity.getType()).thenReturn(ScheduleType.WEEKLY);
            when(entity.getDayOfWeek()).thenReturn(2);
            when(entity.getCronExpression()).thenReturn("0 0 10 ? * TUE");

            when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(entity);
            when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(entity);
            when(notificationMapper.toResponse(any(NotificationEntity.class))).thenReturn(response);

            NotificationResponse result = notificationService.scheduleNotification(request);

            assertNotNull(result);
            verify(notificationProducer).sendToProcess(any(CityRequestDTO.class));
            verify(notificationRepository).save(any(NotificationEntity.class));
        }
    }

    @Nested
    @DisplayName("Update Status Tests")
    class UpdateStatusTests {
        
        @Test
        @DisplayName("Deve atualizar status da notificação com sucesso")
        void success_updateStatus() {
            UUID notificationId = UUID.randomUUID();
            NotificationStatusDTO statusDTO = mock(NotificationStatusDTO.class);
            when(statusDTO.getStatus()).thenReturn(NotificationStatus.EXECUTED);
            
            NotificationEntity existingEntity = mock(NotificationEntity.class);
            when(existingEntity.getNotificationId()).thenReturn(notificationId);
            when(existingEntity.getStatus()).thenReturn(NotificationStatus.PENDING);
            
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(existingEntity));
            when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(existingEntity);
            
            notificationService.updateStatus(notificationId, statusDTO);
            
            verify(notificationRepository).findById(notificationId);
            verify(notificationRepository).save(argThat(notification -> 
                notification.getStatus() == NotificationStatus.EXECUTED
            ));
        }
        
        @Test
        @DisplayName("Deve lançar exceção quando notificação não existe")
        void fail_updateStatus_notificationNotFound() {
            UUID notificationId = UUID.randomUUID();
            NotificationStatusDTO statusDTO = mock(NotificationStatusDTO.class);
            when(statusDTO.getStatus()).thenReturn(NotificationStatus.EXECUTED);
            
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());
            
            assertThrows(IllegalArgumentException.class, () -> 
                notificationService.updateStatus(notificationId, statusDTO)
            );
            
            verify(notificationRepository).findById(notificationId);
            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Cron Expression Tests")
    class CronExpressionTests {

        @Test
        @DisplayName("Deve criar expressão cron diária corretamente")
        void success_createDailyCron() {
            String time = "10:00";
            String expectedCron = "0 0 10 * * ?";

            when(entity.getTime()).thenReturn(time);
            when(entity.getType()).thenReturn(ScheduleType.DAILY);

            when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(entity);
            when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(entity);
            when(notificationMapper.toResponse(any(NotificationEntity.class))).thenReturn(response);

            notificationService.scheduleNotification(request);

            verify(notificationRepository).save(argThat(notification ->
                    notification.getCronExpression().equals(expectedCron)
            ));
        }

        @Test
        @DisplayName("Deve criar expressão cron semanal corretamente")
        void success_createWeeklyCron() {
            request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.WEEKLY,
                    "10:00",
                    2,
                    null,
                    LocalDateTime.now().plusDays(7)
            );

            String expectedCron = "0 0 10 ? * TUE";

            when(entity.getTime()).thenReturn("10:00");
            when(entity.getType()).thenReturn(ScheduleType.WEEKLY);
            when(entity.getDayOfWeek()).thenReturn(2);

            when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(entity);
            when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(entity);
            when(notificationMapper.toResponse(any(NotificationEntity.class))).thenReturn(response);

            notificationService.scheduleNotification(request);

            verify(notificationRepository).save(argThat(notification ->
                    notification.getCronExpression().equals(expectedCron)
            ));
        }

        @Test
        @DisplayName("Não deve criar expressão cron para agendamento único")
        void success_noCreateCron_once() {
            request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.ONCE,
                    null,
                    null,
                    LocalDateTime.now().plusHours(1),
                    null
            );

            when(entity.getType()).thenReturn(ScheduleType.ONCE);
            when(entity.getTime()).thenReturn(null);
            when(entity.getNextExecution()).thenReturn(LocalDateTime.now().plusHours(1));
            when(entity.getCronExpression()).thenReturn(null);

            when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(entity);
            when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(entity);
            when(notificationMapper.toResponse(any(NotificationEntity.class))).thenReturn(response);

            notificationService.scheduleNotification(request);

            verify(notificationRepository).save(argThat(notification ->
                    notification.getCronExpression() == null
            ));
        }
    }
} 