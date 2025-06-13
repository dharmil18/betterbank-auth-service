package com.betterbank.exception;

import com.betterbank.dto.response.ErrorResponse;
import com.betterbank.dto.response.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebExchangeBindException(WebExchangeBindException ex) {
        log.error("Validation Error: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(400, "Validation Failed");

        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            errorResponse.getErrors().add(new ErrorResponse.ValidationError(fieldError.getField(), fieldError.getDefaultMessage()));
        });

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }
}
