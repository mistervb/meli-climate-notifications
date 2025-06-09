package com.mercadolibre.itarc.climatehub_ms_notification.service;

public interface TokenService {
    String getCurrentUserId();
    String getUserIdFromToken(String token);
} 