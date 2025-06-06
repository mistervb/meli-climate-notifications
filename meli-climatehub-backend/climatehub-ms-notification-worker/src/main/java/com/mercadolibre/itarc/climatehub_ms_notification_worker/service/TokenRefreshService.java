package com.mercadolibre.itarc.climatehub_ms_notification_worker.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class TokenRefreshService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:3600000}") // 1 hora por padrão
    private Long expiration;

    public String refreshToken(String oldToken) {
        try {
            // Decodifica o token antigo
            DecodedJWT jwt = JWT.decode(oldToken.replace("Bearer ", ""));
            
            // Extrai as informações necessárias
            String userId = jwt.getClaim("userId").asString();
            
            // Cria um novo token com as mesmas informações mas com nova data de expiração
            return JWT.create()
                    .withIssuer("climatehub-ms-notification-worker")
                    .withClaim("userId", userId)
                    .withIssuedAt(Date.from(Instant.now()))
                    .withExpiresAt(Date.from(Instant.now().plusMillis(expiration)))
                    .sign(Algorithm.HMAC256(secret));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao renovar token", e);
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token.replace("Bearer ", ""));
            return jwt.getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public String extractUserId(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token.replace("Bearer ", ""));
            return jwt.getClaim("userId").asString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao extrair userId do token", e);
        }
    }
} 