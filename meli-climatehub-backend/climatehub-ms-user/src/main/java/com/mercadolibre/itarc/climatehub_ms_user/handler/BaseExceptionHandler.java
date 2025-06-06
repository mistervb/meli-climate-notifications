package com.mercadolibre.itarc.climatehub_ms_user.handler;

import com.mercadolibre.itarc.climatehub_ms_user.controller.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

public abstract class BaseExceptionHandler {

    protected static final String ERROR = "error";
    protected static final String VALIDATION_ERROR = "validation_error";
    protected static final String NOT_FOUND = "not_found";
    protected static final String UNAUTHORIZED = "unauthorized";
    protected static final String FORBIDDEN = "forbidden";
    protected static final String SERVER_ERROR = "server_error";

    /**
     * Cria uma resposta de erro com status HTTP 400 (Bad Request).
     *
     * @param message Mensagem de erro
     * @param request Requisição web
     * @return ResponseEntity com ApiResponse contendo detalhes do erro
     */
    protected ResponseEntity<ApiResponse<String>> badRequest(String message, WebRequest request) {
        return buildErrorResponse(message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Cria uma resposta de erro com status HTTP 404 (Not Found).
     *
     * @param message Mensagem de erro
     * @param request Requisição web
     * @return ResponseEntity com ApiResponse contendo detalhes do erro
     */
    protected ResponseEntity<ApiResponse<String>> notFound(String message, WebRequest request) {
        return buildErrorResponse(message, HttpStatus.NOT_FOUND);
    }

    /**
     * Cria uma resposta de erro com status HTTP 401 (Unauthorized).
     *
     * @param message Mensagem de erro
     * @param request Requisição web
     * @return ResponseEntity com ApiResponse contendo detalhes do erro
     */
    protected ResponseEntity<ApiResponse<String>> unauthorized(String message, WebRequest request) {
        return buildErrorResponse(message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Cria uma resposta de erro com status HTTP 403 (Forbidden).
     *
     * @param message Mensagem de erro
     * @param request Requisição web
     * @return ResponseEntity com ApiResponse contendo detalhes do erro
     */
    protected ResponseEntity<ApiResponse<String>> forbidden(String message, WebRequest request) {
        return buildErrorResponse(message, HttpStatus.FORBIDDEN);
    }

    /**
     * Cria uma resposta de erro com status HTTP 500 (Internal Server Error).
     *
     * @param message Mensagem de erro
     * @param request Requisição web
     * @return ResponseEntity com ApiResponse contendo detalhes do erro
     */
    protected ResponseEntity<ApiResponse<String>> internalServerError(String message, WebRequest request) {
        return buildErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Método utilitário para construir uma resposta de erro padronizada.
     *
     * @param message Mensagem de erro
     * @param status Status HTTP
     * @return ResponseEntity com ApiResponse contendo detalhes do erro
     */
    private ResponseEntity<ApiResponse<String>> buildErrorResponse(String message, HttpStatus status) {
        String statusType = getStatusType(status);
        ApiResponse<String> apiResponse = new ApiResponse<>(statusType, status.value(), message);
        return new ResponseEntity<>(apiResponse, status);
    }

    /**
     * Retorna o tipo de status com base no status HTTP.
     *
     * @param status Status HTTP
     * @return Tipo de status
     */
    private String getStatusType(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            return NOT_FOUND;
        } else if (status == HttpStatus.UNAUTHORIZED) {
            return UNAUTHORIZED;
        } else if (status == HttpStatus.FORBIDDEN) {
            return FORBIDDEN;
        } else if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            return SERVER_ERROR;
        } else if (status == HttpStatus.BAD_REQUEST) {
            return ERROR;
        } else {
            return ERROR;
        }
    }
}
