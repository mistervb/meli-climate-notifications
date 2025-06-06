package com.mercadolibre.itarc.climatehub_ms_user.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {
    
    @NotBlank(message = "O nome é obrigatório")
    private String name;
    
    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Email inválido")
    private String email;
    
    @NotBlank(message = "A senha é obrigatória")
    private String password;
} 