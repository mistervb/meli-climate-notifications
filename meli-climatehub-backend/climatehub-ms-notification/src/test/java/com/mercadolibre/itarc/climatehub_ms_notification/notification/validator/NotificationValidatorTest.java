package com.mercadolibre.itarc.climatehub_ms_notification.notification.validator;

import com.mercadolibre.itarc.climatehub_ms_notification.constants.ScheduleType;
import com.mercadolibre.itarc.climatehub_ms_notification.exception.BusinessException;
import com.mercadolibre.itarc.climatehub_ms_notification.model.dto.NotificationRequest;

import com.mercadolibre.itarc.climatehub_ms_notification.validator.notification.NotificationValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

@DisplayName("NotificationValidator Tests")
public class NotificationValidatorTest {

    @Nested
    @DisplayName("Required Fields Validation")
    class RequiredFieldsValidation {
        @Test
        @DisplayName("Deve falhar quando cidade é nula")
        void fail_validateCityName_cidadeNula() {
            NotificationRequest request = new NotificationRequest(
                    null,
                    "SP",
                    ScheduleType.ONCE,
                    null,
                    null,
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("City name cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Deve falhar quando cidade está vazia")
        void fail_validateCityName_cidadeVazia() {
            NotificationRequest request = new NotificationRequest(
                    "",
                    "SP",
                    ScheduleType.ONCE,
                    null,
                    null,
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("City name cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Deve falhar quando UF é nula")
        void fail_validateUF_ufNula() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    null,
                    ScheduleType.ONCE,
                    null,
                    null,
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("UF cannot be null or empty", exception.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"ABC", "S", "123", "SP "})
        @DisplayName("Deve falhar quando UF é inválida")
        void fail_validateUF_formatoInvalido(String uf) {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    uf,
                    ScheduleType.ONCE,
                    null,
                    null,
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("UF must be 2 uppercase letters (e.g., 'SP')", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Schedule Type Validation")
    class ScheduleTypeValidation {
        @Test
        @DisplayName("Deve passar na validação de agendamento único")
        void success_validateOnceSchedule_dadosValidos() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.ONCE,
                    null,
                    null,
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );

            Assertions.assertDoesNotThrow(() -> NotificationValidator.validateNotificationRequest(request));
        }

        @Test
        @DisplayName("Deve falhar quando agendamento único tem data no passado")
        void fail_validateOnceSchedule_dataPassada() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.ONCE,
                    null,
                    null,
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("executeAt must be a future date/time", exception.getMessage());
        }

        @Test
        @DisplayName("Deve falhar quando agendamento único tem campos extras")
        void fail_validateOnceSchedule_camposExtras() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.ONCE,
                    "10:00",
                    null,
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("ONCE schedule should only contain executeAt", exception.getMessage());
        }

        @Test
        @DisplayName("Deve passar na validação de agendamento diário")
        void success_validateDailySchedule_dadosValidos() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.DAILY,
                    "10:00",
                    null,
                    null,
                    LocalDateTime.now().plusDays(1)
            );

            Assertions.assertDoesNotThrow(() -> NotificationValidator.validateNotificationRequest(request));
        }

        @Test
        @DisplayName("Deve falhar quando agendamento semanal não tem dia da semana")
        void fail_validateWeeklySchedule_semDiaSemana() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.WEEKLY,
                    "10:00",
                    null,
                    null,
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("dayOfWeek is required for WEEKLY schedule type", exception.getMessage());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 8, -1, 10})
        @DisplayName("Deve falhar quando dia da semana é inválido")
        void fail_validateWeeklySchedule_diaSemanaInvalido(int dayOfWeek) {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.WEEKLY,
                    "10:00",
                    dayOfWeek,
                    null,
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("dayOfWeek must be between 1 (Monday) and 7 (Sunday)", exception.getMessage());
        }

        @Test
        @DisplayName("Deve passar na validação de agendamento personalizado")
        void success_validateCustomSchedule_cronValido() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.CUSTOM,
                    null,
                    null,
                    null,
                    LocalDateTime.now().plusDays(1)
            );

            Assertions.assertDoesNotThrow(() -> NotificationValidator.validateNotificationRequest(request));
        }

        @Test
        @DisplayName("Deve falhar quando expressão cron é inválida")
        void fail_validateCustomSchedule_cronInvalido() {
            NotificationRequest request =  new NotificationRequest(
                    null,                               // cityName
                    "SP",                              // uf
                    ScheduleType.ONCE,                 // scheduleType
                    null,                              // time
                    null,                              // dayOfWeek
                    LocalDateTime.now().plusHours(1),  // executeAt
                    LocalDateTime.now().plusDays(1)    // endDate
            );

            Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
        }
    }

    @Nested
    @DisplayName("Time Format Validation")
    class TimeFormatValidation {
        @ParameterizedTest
        @ValueSource(strings = {"23:00", "00:00", "05:59", "22:01"})
        @DisplayName("Deve falhar quando horário está fora do intervalo permitido")
        void fail_validateTimeRange_foraIntervalo(String time) {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.DAILY,
                    time,
                    null,
                    null,
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("time must be between 06:00 and 22:00", exception.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {":", "10", "abc", "aa:bb", "25:00", "10:60", "99:99", "10:0"})
        @DisplayName("Deve falhar quando formato do horário é inválido")
        void fail_validateTimeFormat_formatoInvalido(String time) {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.DAILY,
                    time,
                    null,
                    null,
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("time must be in HH:mm format (24-hour)", exception.getMessage());
        }

        @Test
        @DisplayName("Deve falhar quando hora não tem dois dígitos")
        void fail_validateTimeFormat_horaUmDigito() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.DAILY,
                    "1:00",
                    null,
                    null,
                    LocalDateTime.now().plusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("Invalid time format: Text '1:00' could not be parsed at index 0", exception.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"06:00", "10:30", "15:45", "22:00"})
        @DisplayName("Deve passar quando horário está no formato e intervalo corretos")
        void success_validateTime_formatoValido(String time) {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.DAILY,
                    time,
                    null,
                    null,
                    LocalDateTime.now().plusDays(1)
            );

            Assertions.assertDoesNotThrow(() -> NotificationValidator.validateNotificationRequest(request));
        }
    }

    @Nested
    @DisplayName("End Date Validation")
    class EndDateValidation {
        @Test
        @DisplayName("Deve falhar quando data final é mais de um ano no futuro")
        void fail_validateEndDate_maisDeUmAno() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.DAILY,
                    "10:00",
                    null,
                    null,
                    LocalDateTime.now().plusYears(2)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("endDate cannot be more than 1 year in the future", exception.getMessage());
        }

        @Test
        @DisplayName("Deve falhar quando data final está no passado")
        void fail_validateEndDate_dataPassada() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.DAILY,
                    "10:00",
                    null,
                    null,
                    LocalDateTime.now().minusDays(1)
            );

            BusinessException exception = Assertions.assertThrows(
                    BusinessException.class,
                    () -> NotificationValidator.validateNotificationRequest(request)
            );
            Assertions.assertEquals("endDate must be a future date/time", exception.getMessage());
        }

        @Test
        @DisplayName("Deve passar quando data final está dentro do limite de um ano")
        void success_validateEndDate_dentroPrazo() {
            NotificationRequest request = new NotificationRequest(
                    "São Paulo",
                    "SP",
                    ScheduleType.DAILY,
                    "10:00",
                    null,
                    null,
                    LocalDateTime.now().plusMonths(6)
            );

            Assertions.assertDoesNotThrow(() -> NotificationValidator.validateNotificationRequest(request));
        }
    }
}
