package com.mercadolibre.itarc.climatehub_ms_user.useraccount.it;

import com.mercadolibre.itarc.climatehub_ms_user.controller.dto.ApiResponse;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserCreatedDTO;
import com.mercadolibre.itarc.climatehub_ms_user.model.dto.UserPayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserIT {
    @LocalServerPort
    private Integer port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return String.format("http://localhost:%d/user", port);
    }

    private HttpEntity<UserPayload> createRequestEntity(UserPayload body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    @DisplayName("Deve registrar um usuário com sucesso e retornar 201 Created")
    void success_201_registerUser() {
        UserPayload body = new UserPayload("Vitor", "vitor@gmail.com", "Senha@1235");
        HttpEntity<UserPayload> request = createRequestEntity(body);
        
        ResponseEntity<ApiResponse<UserCreatedDTO>> response = restTemplate.exchange(
            getBaseUrl() + "/register",
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<ApiResponse<UserCreatedDTO>>() {}
        );

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        ApiResponse<UserCreatedDTO> apiResponse = response.getBody();
        Assertions.assertNotNull(apiResponse);
        Assertions.assertEquals("SUCCESS", apiResponse.status());
        Assertions.assertEquals(HttpStatus.CREATED.value(), apiResponse.statusCode());
        
        UserCreatedDTO userData = apiResponse.data();
        Assertions.assertNotNull(userData);
        Assertions.assertEquals("Vitor", userData.username());
    }

    @Test
    @DisplayName("Deve falhar ao tentar registrar usuário com username nulo")
    void fail_400_registerUser_username_nulo() {
        UserPayload body = new UserPayload(null, "vitor@gmail.com", "Senha@1235");
        HttpEntity<UserPayload> request = createRequestEntity(body);
        
        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
            getBaseUrl() + "/register",
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ApiResponse<String> apiResponse = response.getBody();
        Assertions.assertNotNull(apiResponse);
        Assertions.assertEquals("error", apiResponse.status());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), apiResponse.statusCode());
        Assertions.assertEquals("Username is required.", apiResponse.data());
    }

    @Test
    @DisplayName("Deve falhar ao tentar registrar usuário com email inválido")
    void fail_400_registerUser_email_invalido() {
        UserPayload body = new UserPayload("Vitor", "email-invalido", "Senha@1235");
        HttpEntity<UserPayload> request = createRequestEntity(body);
        
        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
            getBaseUrl() + "/register",
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ApiResponse<String> apiResponse = response.getBody();
        Assertions.assertNotNull(apiResponse);
        Assertions.assertEquals("error", apiResponse.status());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), apiResponse.statusCode());
        Assertions.assertEquals("The email provided must be valid", apiResponse.data());
    }

    @Test
    @DisplayName("Deve falhar ao tentar registrar usuário com senha fraca")
    void fail_400_registerUser_senha_fraca() {
        UserPayload body = new UserPayload("Vitor", "vitor@gmail.com", "123456");
        HttpEntity<UserPayload> request = createRequestEntity(body);
        
        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
            getBaseUrl() + "/register",
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ApiResponse<String> apiResponse = response.getBody();
        Assertions.assertNotNull(apiResponse);
        Assertions.assertEquals("error", apiResponse.status());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), apiResponse.statusCode());
        Assertions.assertEquals(
            "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character.", 
            apiResponse.data()
        );
    }

    @Test
    @DisplayName("Deve falhar ao tentar registrar usuário com email já existente")
    void fail_400_registerUser_email_duplicado() {
        // Primeiro registro (sucesso)
        UserPayload firstUser = new UserPayload("Vitor", "vitor2@gmail.com", "Senha@1235");
        HttpEntity<UserPayload> firstRequest = createRequestEntity(firstUser);
        
        // Faz a primeira requisição sem tentar deserializar a resposta
        ResponseEntity<String> firstResponse = restTemplate.exchange(
            getBaseUrl() + "/register",
            HttpMethod.POST,
            firstRequest,
            String.class
        );
        
        Assertions.assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());

        // Tentativa de registro com mesmo email
        UserPayload duplicateUser = new UserPayload("Outro Nome", "vitor2@gmail.com", "Senha@1235");
        HttpEntity<UserPayload> duplicateRequest = createRequestEntity(duplicateUser);
        
        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
            getBaseUrl() + "/register",
            HttpMethod.POST,
            duplicateRequest,
            new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ApiResponse<String> apiResponse = response.getBody();
        Assertions.assertNotNull(apiResponse);
        Assertions.assertEquals("error", apiResponse.status());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), apiResponse.statusCode());
        Assertions.assertEquals("Email already exists", apiResponse.data());
    }
}
