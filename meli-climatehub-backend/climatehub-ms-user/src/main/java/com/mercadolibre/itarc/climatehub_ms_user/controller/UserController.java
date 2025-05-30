package com.mercadolibre.itarc.climatehub_ms_user.controller;

import com.mercadolibre.itarc.climatehub_ms_user.controller.dto.ApiResponse;
import com.mercadolibre.itarc.climatehub_ms_user.controller.dto.AuthRequest;
import com.mercadolibre.itarc.climatehub_ms_user.controller.dto.AuthResponse;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserCreatedDTO;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserPayload;
import com.mercadolibre.itarc.climatehub_ms_user.service.user.UserService;
import com.mercadolibre.itarc.climatehub_ms_user.util.JwtUtil;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public UserController(
        UserService userService,
        AuthenticationManager authenticationManager,
        JwtUtil jwtUtil
    ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserCreatedDTO>> registerUser(@RequestBody UserPayload userPayload) {
        UserCreatedDTO createdUser = userService.registerUser(userPayload);
        ApiResponse<UserCreatedDTO> response = new ApiResponse<>("SUCCESS", HttpStatus.CREATED.value(), createdUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{userId}/opt-out")
    public ResponseEntity<Void> optOut(@PathVariable UUID userId) {
        userService.setNotificationOptOut(userId, true);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/opt-in")
    public ResponseEntity<Void> optIn(@PathVariable UUID userId) {
        userService.setNotificationOptOut(userId, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            logger.info("Tentativa de login para o email: {}", request.email());
            
            UsernamePasswordAuthenticationToken authInputToken =
                new UsernamePasswordAuthenticationToken(request.email(), request.password());

            authenticationManager.authenticate(authInputToken);
            logger.info("Autenticação bem-sucedida para o email: {}", request.email());

            String token = jwtUtil.generateToken(request.email(), String.valueOf(userService.getUserIdByEmail(request.email())));
            return ResponseEntity.ok(new ApiResponse<>("success", HttpStatus.OK.value(), new AuthResponse(token)));
        } catch (BadCredentialsException e) {
            logger.warn("Credenciais inválidas para o email: {}", request.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>("error", HttpStatus.UNAUTHORIZED.value(), "Usuário ou senha inválidos"));
        } catch (AuthenticationException e) {
            logger.error("Erro de autenticação para o email: {}", request.email(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>("error", HttpStatus.UNAUTHORIZED.value(), "Erro de autenticação: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro interno durante o login para o email: {}", request.email(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("server_error", HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno do servidor: " + e.getMessage()));
        }
    }
}
