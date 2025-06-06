package com.mercadolibre.itarc.climatehub_ms_user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserRequestDTO;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserResponseDTO;
import com.mercadolibre.itarc.climatehub_ms_user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class UserControllerIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUserSuccessfully() throws Exception {
        // Given
        UserRequestDTO request = UserRequestDTO.builder()
                .name("Victor Test")
                .email("victor@test.com")
                .password("123456")
                .build();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then
        UserResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                UserResponseDTO.class
        );

        assertNotNull(response.getId());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getEmail(), response.getEmail());
        assertFalse(response.getOptOut());
    }

    @Test
    void shouldUpdateOptOutStatusSuccessfully() throws Exception {
        // Given
        UserRequestDTO request = UserRequestDTO.builder()
                .name("Victor Test")
                .email("victor@test.com")
                .password("123456")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponseDTO createdUser = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                UserResponseDTO.class
        );

        // When
        mockMvc.perform(patch("/api/v1/users/" + createdUser.getId() + "/opt-out")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"optOut\": true}"))
                .andExpect(status().isOk());

        // Then
        MvcResult getResult = mockMvc.perform(get("/api/v1/users/" + createdUser.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        UserResponseDTO updatedUser = objectMapper.readValue(
                getResult.getResponse().getContentAsString(),
                UserResponseDTO.class
        );

        assertTrue(updatedUser.getOptOut());
    }

    @Test
    void shouldReturnNotFoundForNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/999999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestForInvalidEmail() throws Exception {
        // Given
        UserRequestDTO request = UserRequestDTO.builder()
                .name("Victor Test")
                .email("invalid-email")
                .password("123456")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
} 