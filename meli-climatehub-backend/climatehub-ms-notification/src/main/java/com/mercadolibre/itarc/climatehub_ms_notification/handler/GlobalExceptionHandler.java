package com.mercadolibre.itarc.climatehub_ms_notification.handler;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.mercadolibre.itarc.climatehub_ms_notification.controller.dto.ApiResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.exception.BusinessException;
import com.mercadolibre.itarc.climatehub_ms_notification.exception.InvalidPathException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    private boolean isSseRequest(WebRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    /**
     * Trata exceções de negócio (BusinessException).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<String>> handleBusinessException(BusinessException ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }
        return badRequest(ex.getMessage(), request);
    }

    /**
     * Trata exceções de entidade não encontrada.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }
        return notFound(ex.getMessage(), request);
    }

    /**
     * Trata exceções de validação de argumentos de método.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ApiResponse<Map<String, String>> apiResponse = new ApiResponse<>(
                VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST.value(),
                errors
        );

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Trata exceções de tipo incompatível de argumentos de método.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }

        String message = String.format("The parameter '%s' with value '%s' could not be converted to type '%s'",
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());

        return badRequest(message, request);
    }

    /**
     * Trata exceções de parâmetros obrigatórios ausentes.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }

        String message = String.format("The parameter '%s' is required", ex.getParameterName());
        return badRequest(message, request);
    }

    /**
     * Trata exceções de mensagem HTTP não legível.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }
        return badRequest("Invalid request format", request);
    }

    /**
     * Trata exceções de violação de integridade de dados.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }
        return badRequest("Data integrity violation", request);
    }

    /**
     * Trata exceções de tamanho máximo de upload excedido.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<String>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }
        return badRequest("Maximum upload size exceeded", request);
    }

    /**
     * Trata exceções de handler não encontrado.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }

        String message = String.format("No handler found for %s %s",
                ex.getHttpMethod(), ex.getRequestURL());

        return notFound(message, request);
    }

    /**
     * Trata todas as outras exceções não mapeadas.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleAllUncaughtException(Exception ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }
        return internalServerError("An internal server error occurred", request);
    }
    
    /**
     * Handles invalid path exceptions.
     */
    @ExceptionHandler(InvalidPathException.class)
    public ResponseEntity<ApiResponse<String>> handleInvalidPathException(InvalidPathException ex, WebRequest request) {
        if (isSseRequest(request)) {
            return null;
        }
        return badRequest(ex.getMessage(), request);
    }

    /**
     * Handles out of memory errors.
     */
    @ExceptionHandler(OutOfMemoryError.class)
    public ResponseEntity<ApiResponse<String>> handleOutOfMemoryError(OutOfMemoryError ex, WebRequest request) {
        // Force garbage collection to try to free up memory
        System.gc();
        
        if (isSseRequest(request)) {
            return null;
        }
        return internalServerError("Ocorreu um erro ao processar a requisição. Memória insuficiente.", request);
    }
}

