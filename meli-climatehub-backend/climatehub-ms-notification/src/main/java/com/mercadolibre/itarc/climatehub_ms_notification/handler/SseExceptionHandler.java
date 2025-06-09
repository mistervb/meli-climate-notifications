package com.mercadolibre.itarc.climatehub_ms_notification.handler;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@ControllerAdvice
public class SseExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseBodyEmitter handleException(Exception ex) {
        SseEmitter emitter = new SseEmitter();
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(ex.getMessage())
                    .id("error-" + System.currentTimeMillis())
                    .reconnectTime(3000L));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
} 