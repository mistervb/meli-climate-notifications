package com.mercadolibre.itarc.climatehub_ms_notification.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.mercadolibre.itarc.climatehub_ms_notification.config.RequestInterceptor;
import com.mercadolibre.itarc.climatehub_ms_notification.util.JwtUtil;

@Service
public class TokenService {

    @Autowired
    private JwtUtil jwtUtil;

    public String getCurrentUserId() {
        String token = RequestInterceptor.getCurrentToken();
        return jwtUtil.extractUserId(token);
    }
} 