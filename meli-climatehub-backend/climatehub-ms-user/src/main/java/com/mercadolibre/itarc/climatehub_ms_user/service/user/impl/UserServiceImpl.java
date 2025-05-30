package com.mercadolibre.itarc.climatehub_ms_user.service.user.impl;

import com.mercadolibre.itarc.climatehub_ms_user.exception.BusinessException;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserCreatedDTO;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserPayload;
import com.mercadolibre.itarc.climatehub_ms_user.model.entity.UserEntity;
import com.mercadolibre.itarc.climatehub_ms_user.model.mapper.UserMapper;
import com.mercadolibre.itarc.climatehub_ms_user.repository.UserRepository;
import com.mercadolibre.itarc.climatehub_ms_user.service.RedisOptOutService;
import com.mercadolibre.itarc.climatehub_ms_user.service.user.UserService;
import com.mercadolibre.itarc.climatehub_ms_user.validator.UserValidator;

import jakarta.persistence.EntityNotFoundException;

import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RedisOptOutService redisOptOutService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
        UserRepository userRepository, 
        UserMapper userMapper, 
        RedisOptOutService redisOptOutService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.redisOptOutService = redisOptOutService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserCreatedDTO registerUser(UserPayload payload) {
        UserValidator.validateUsername(payload.username());
        UserValidator.validateEmail(payload.email());
        UserValidator.validatePassword(payload.passwordHashed());

        if (userRepository.existsByEmail(payload.email())) {
            throw new BusinessException("Email already exists");
        }
        UserEntity user = userMapper.toEntity(payload);
        user.setPasswordHashed(passwordEncoder.encode(payload.passwordHashed()));
        
        UserEntity savedUser = userRepository.save(user);
        
        // Inicializa o opt-out no Redis com o valor padrão (false)
        redisOptOutService.setOptOut(savedUser.getUserId(), false);
        
        return userMapper.toCreateData(savedUser);
    }

    public void setNotificationOptOut(UUID userId, boolean optOut) {
        UserEntity user = findUserById(userId);
        user.setNotificationOptOut(optOut);
        userRepository.save(user);

        redisOptOutService.setOptOut(userId, optOut); // grava no Redis
    }

    @Cacheable(value = "user", key = "#userId", unless = "#result == null")
    private UserEntity findUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(
            () -> new EntityNotFoundException("User with id \"" + userId + "\" not found.")
        );
    }

    private UserEntity findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
            () -> new EntityNotFoundException("User with email \"" + email + "\" not found.")
        );
    }

    @Override
    public UUID getUserIdByEmail(String email) {
        return findUserByEmail(email).getUserId();
    }
}
