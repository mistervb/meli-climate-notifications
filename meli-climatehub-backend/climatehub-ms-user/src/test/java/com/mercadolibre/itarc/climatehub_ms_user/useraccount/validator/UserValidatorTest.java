package com.mercadolibre.itarc.climatehub_ms_user.useraccount.validator;

import com.mercadolibre.itarc.climatehub_ms_user.exception.BusinessException;
import com.mercadolibre.itarc.climatehub_ms_user.validator.UserValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UserValidatorTest {
    @Test
    @DisplayName("Deve passar na validação de username válido")
    void success_validateUsername() {
        Assertions.assertDoesNotThrow(() -> UserValidator.validateUsername("Victor"));
    }

    @Test
    @DisplayName("Deve falhar na validação de username nulo")
    void fail_validateUsername_nulo() {
        BusinessException exception = Assertions.assertThrows(
            BusinessException.class,
            () -> UserValidator.validateUsername(null)
        );
        Assertions.assertEquals("Username is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve falhar na validação de username vazio")
    void fail_validateUsername_vazio() {
        BusinessException exception = Assertions.assertThrows(
            BusinessException.class,
            () -> UserValidator.validateUsername("  ")
        );
        Assertions.assertEquals("Username is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve passar na validação de email válido")
    void success_validateEmail() {
        Assertions.assertDoesNotThrow(() -> UserValidator.validateEmail("victor@email.com"));
    }

    @Test
    @DisplayName("Deve falhar na validação de email nulo")
    void fail_validateEmail_nulo() {
        BusinessException exception = Assertions.assertThrows(
            BusinessException.class,
            () -> UserValidator.validateEmail(null)
        );
        Assertions.assertEquals("Email is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve falhar na validação de email inválido")
    void fail_validateEmail_invalido() {
        BusinessException exception = Assertions.assertThrows(
            BusinessException.class,
            () -> UserValidator.validateEmail("email-invalido")
        );
        Assertions.assertEquals("The email provided must be valid", exception.getMessage());
    }

    @Test
    @DisplayName("Deve passar na validação de senha válida")
    void success_validatePassword() {
        Assertions.assertDoesNotThrow(() -> UserValidator.validatePassword("testSenha@123"));
    }

    @Test
    @DisplayName("Deve falhar na validação de senha nula")
    void fail_validatePassword_nulo() {
        BusinessException exception = Assertions.assertThrows(
            BusinessException.class,
            () -> UserValidator.validatePassword(null)
        );
        Assertions.assertEquals("Password is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve falhar na validação de senha com menos de 8 caracteres")
    void fail_validatePassword_menos_8_caracteres() {
        BusinessException exception = Assertions.assertThrows(
            BusinessException.class,
            () -> UserValidator.validatePassword("Vitin@1")
        );
        Assertions.assertEquals("Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve falhar na validação de senha sem letra maiúscula")
    void fail_validatePassword_sem_letra_maiuscula() {
        BusinessException exception = Assertions.assertThrows(
            BusinessException.class,
            () -> UserValidator.validatePassword("vitin@12344")
        );
        Assertions.assertEquals("Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve falhar na validação de senha sem caractere especial")
    void fail_validatePassword_sem_caractere_especial() {
        BusinessException exception = Assertions.assertThrows(
            BusinessException.class,
            () -> UserValidator.validatePassword("Vitin12344")
        );
        Assertions.assertEquals("Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve falhar na validação de senha sem número")
    void fail_validatePassword_sem_numero() {
        BusinessException exception = Assertions.assertThrows(
            BusinessException.class,
            () -> UserValidator.validatePassword("Vitin@developer")
        );
        Assertions.assertEquals("Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character.", exception.getMessage());
    }
}
