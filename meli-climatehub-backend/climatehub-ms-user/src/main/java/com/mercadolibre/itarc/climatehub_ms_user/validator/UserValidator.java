package com.mercadolibre.itarc.climatehub_ms_user.validator;

import com.mercadolibre.itarc.climatehub_ms_user.exception.BusinessException;

public class UserValidator {
    public static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("Username is required.");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Email is required.");
        }
        
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(emailRegex)) {
            throw new BusinessException("The email provided must be valid");
        }
    }

    public static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException("Password is required.");
        }
        
        String regex = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!password.matches(regex)) {
            throw new BusinessException("Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character.");
        }
    }
}
