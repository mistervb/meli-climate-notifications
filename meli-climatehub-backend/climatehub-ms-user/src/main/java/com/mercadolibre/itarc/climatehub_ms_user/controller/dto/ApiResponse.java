package com.mercadolibre.itarc.climatehub_ms_user.controller.dto;

public record ApiResponse<T>(
    String status,
    int statusCode,
    T data
) {}

