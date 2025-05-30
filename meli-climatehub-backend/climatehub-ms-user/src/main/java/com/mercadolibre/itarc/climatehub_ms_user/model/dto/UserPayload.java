package com.mercadolibre.itarc.climatehub_ms_user.model.dto;

public record UserPayload(
        String username,
        String email,
        String passwordHashed
) { }
