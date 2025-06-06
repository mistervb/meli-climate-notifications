package com.mercadolibre.itarc.climatehub_ms_notification.service.impl;

import org.springframework.stereotype.Service;

import com.mercadolibre.itarc.climatehub_ms_notification.config.RequestInterceptor;
import com.mercadolibre.itarc.climatehub_ms_notification.service.TokenService;
import com.mercadolibre.itarc.climatehub_ms_notification.util.JwtUtil;

@Service
public class TokenServiceImpl implements TokenService {
    private final JwtUtil jwtUtil;

    public TokenServiceImpl(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String getCurrentUserId() {
        String token = RequestInterceptor.getCurrentToken();
        return jwtUtil.extractUserId(token);
    }
}
