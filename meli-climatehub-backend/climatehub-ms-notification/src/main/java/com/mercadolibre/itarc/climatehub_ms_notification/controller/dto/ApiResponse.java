package com.mercadolibre.itarc.climatehub_ms_notification.controller.dto;
 
public record ApiResponse<T>(
    String status,
    int statusCode,
    T data
) {}