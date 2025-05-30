package com.mercadolibre.itarc.climatehub_ms_notification_worker.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

@Component
public class TokenEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private final SecretKeySpec secretKey;

    public TokenEncryptionUtil(@Value("${jwt.schedule.secret}") String secret) {
        this.secretKey = createSecretKey(secret);
    }

    private SecretKeySpec createSecretKey(String secret) {
        try {
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            return new SecretKeySpec(key, ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao criar chave de criptografia", e);
        }
    }

    public String encrypt(String token) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(token.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar token", e);
        }
    }

    public String decrypt(String encryptedToken) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedToken)));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao descriptografar token", e);
        }
    }
} 