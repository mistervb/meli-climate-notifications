package com.mercadolibre.itarc.climatehub_ms_notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.itarc.climatehub_ms_notification.constants.ScheduleType;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class NotificationControllerIT {

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.11-management")
            .withExposedPorts(5672, 15672);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void shouldCreateDailyNotificationSuccessfully() throws Exception {
        // Given
        NotificationRequest request = new NotificationRequest(
            "São Paulo",
            "SP",
            ScheduleType.DAILY,
            "08:00",
            null,
            null,
            null
        );

        // When
        MvcResult result = mockMvc.perform(post("/notification/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        NotificationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                NotificationResponse.class
        );

        assertNotNull(response.notificationId());
        assertNotNull(response.nextExecution());

        verify(rabbitTemplate).convertAndSend(any(), any(), any(NotificationRequest.class));
    }

    @Test
    void shouldCreateWeeklyNotificationSuccessfully() throws Exception {
        // Given
        NotificationRequest request = new NotificationRequest(
            "São Paulo",
            "SP",
            ScheduleType.WEEKLY,
            "08:00",
            DayOfWeek.MONDAY.getValue(),
            null,
            null
        );

        // When
        MvcResult result = mockMvc.perform(post("/notification/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        NotificationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                NotificationResponse.class
        );

        assertNotNull(response.notificationId());
        assertNotNull(response.nextExecution());

        verify(rabbitTemplate).convertAndSend(any(), any(), any(NotificationRequest.class));
    }

    @Test
    void shouldCreateOneTimeNotificationSuccessfully() throws Exception {
        // Given
        LocalDateTime executeAt = LocalDateTime.now().plusHours(1);
        NotificationRequest request = new NotificationRequest(
            "São Paulo",
            "SP",
            ScheduleType.ONCE,
            null,
            null,
            executeAt,
            null
        );

        // When
        MvcResult result = mockMvc.perform(post("/notification/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        NotificationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                NotificationResponse.class
        );

        assertNotNull(response.notificationId());
        assertNotNull(response.nextExecution());
        assertEquals(executeAt, response.nextExecution());

        verify(rabbitTemplate).convertAndSend(any(), any(), any(NotificationRequest.class));
    }

    @Test
    void shouldReturnBadRequestForInvalidScheduleType() throws Exception {
        // Given
        NotificationRequest request = new NotificationRequest(
            "São Paulo",
            "SP",
            null, // Invalid schedule type
            "08:00",
            null,
            null,
            null
        );

        // When/Then
        mockMvc.perform(post("/notification/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMissingRequiredFields() throws Exception {
        // Given
        NotificationRequest request = new NotificationRequest(
            "São Paulo",
            null, // Missing UF
            ScheduleType.DAILY,
            "08:00",
            null,
            null,
            null
        );

        // When/Then
        mockMvc.perform(post("/notification/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
} 