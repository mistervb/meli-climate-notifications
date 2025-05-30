package com.mercadolibre.itarc.climatehub_ms_user.useraccount.service;

import com.mercadolibre.itarc.climatehub_ms_user.exception.BusinessException;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserCreatedDTO;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserPayload;
import com.mercadolibre.itarc.climatehub_ms_user.model.entity.UserEntity;
import com.mercadolibre.itarc.climatehub_ms_user.model.mapper.UserMapper;
import com.mercadolibre.itarc.climatehub_ms_user.repository.UserRepository;
import com.mercadolibre.itarc.climatehub_ms_user.service.user.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {
    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    private UserPayload userPayload;
    private UUID currentUserId;
    private UserCreatedDTO userCreatedDTOExpected;

    @BeforeEach
    void setUp() {
        this.userPayload = new UserPayload(
                "Yui Takashi",
                "takashi.yui@gmail.com",
                "Teste@Senha123");

        this.userCreatedDTOExpected = new UserCreatedDTO(
                UUID.randomUUID(),
                "Yui Takashi",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private void mockUserRepositorySave() {
        this.currentUserId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(userPayload.username());
        userEntity.setEmail(userPayload.email());
        userEntity.setPasswordHashed(userPayload.passwordHashed());
        userEntity.setCreatedAt(LocalDateTime.now());
        userEntity.setUpdatedAt(LocalDateTime.now());

        Mockito.when(userMapper.toEntity(userPayload)).thenReturn(userEntity);

        userEntity.setUserId(this.currentUserId);
        Mockito.when(userRepository.save(userEntity)).thenReturn(userEntity);
        
        UserCreatedDTO userCreatedDTO = new UserCreatedDTO(
            this.currentUserId,
            userEntity.getUsername(),
            userEntity.getCreatedAt(),
            userEntity.getUpdatedAt()
        );
        Mockito.when(userMapper.toCreateData(userEntity)).thenReturn(userCreatedDTO);

        Mockito.when(userRepository.existsByEmail(userPayload.email()))
            .thenReturn(false);
    }

    @Test
    @DisplayName("Deve registrar um usuário no banco de dados com sucesso.")
    void success_registerUser() {
        mockUserRepositorySave();

        UserCreatedDTO created = userService.registerUser(this.userPayload);

        Assertions.assertNotNull(created);
        Assertions.assertEquals(this.currentUserId, created.userId());
        Assertions.assertEquals(this.userPayload.username(), created.username());
    }

    @Test
    @DisplayName("Deve falhar ao registrar um usuário com dados nulos ou vazios.")
    void fail_registerUser_dados_nulos_e_vazios() {
        UserPayload payloadUsernameNulo = new UserPayload(
            null,
            "takashi.yui@gmail.com",
            "Teste@Senha123"
        );

        UserPayload payloadUsernameVazio = new UserPayload(
                "",
                "takashi.yui@gmail.com",
                "Teste@Senha123"
        );

        UserPayload payloadEmailNulo = new UserPayload(
                "Yui Takashi",
                null,
                "Teste@Senha123"
        );

        UserPayload payloadEmailVazio = new UserPayload(
                "Yui Takashi",
                null,
                "Teste@Senha123"
        );


        UserPayload payloadEmailInvalido = new UserPayload(
                "Yui Takashi",
                "takashi.yui.com",
                "Teste@Senha123"
        );

        UserPayload payloadSenhaNula = new UserPayload(
                "Yui Takashi",
                "takashi.yui@gmail.com",
                null
        );

        UserPayload payloadSenhaVazia = new UserPayload(
                "Yui Takashi",
                "takashi.yui@gmail.com",
                ""
        );

        UserPayload payloadSenhaVaziaComEspacos = new UserPayload(
                "Yui Takashi",
                "takashi.yui@gmail.com",
                "     "
        );

        UserPayload payloadSenhaInvalida = new UserPayload(
                "Yui Takashi",
                "takashi.yui@gmail.com",
                "123456"
        );


        Assertions.assertThrows(BusinessException.class,
            () -> userService.registerUser(payloadUsernameNulo));
        Assertions.assertThrows(BusinessException.class,
            () -> userService.registerUser(payloadUsernameVazio));
        Assertions.assertThrows(BusinessException.class,
                () -> userService.registerUser(payloadEmailNulo));
        Assertions.assertThrows(BusinessException.class,
                () -> userService.registerUser(payloadEmailVazio));
        Assertions.assertThrows(BusinessException.class,
                () -> userService.registerUser(payloadEmailInvalido));
        Assertions.assertThrows(BusinessException.class,
                () -> userService.registerUser(payloadSenhaNula));
        Assertions.assertThrows(BusinessException.class,
                () -> userService.registerUser(payloadSenhaVazia));
        Assertions.assertThrows(BusinessException.class,
                () -> userService.registerUser(payloadSenhaVaziaComEspacos));
        Assertions.assertThrows(BusinessException.class,
                () -> userService.registerUser(payloadSenhaInvalida));
    }
}
