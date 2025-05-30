package com.mercadolibre.itarc.climatehub_ms_notification.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    public String extractUserId(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer("climatehub-ms-user")
                    .build();

            DecodedJWT jwt = verifier.verify(token);
            return jwt.getClaim("userId").asString();
        } catch (JWTVerificationException e) {
            return null;
        }
    }
} 