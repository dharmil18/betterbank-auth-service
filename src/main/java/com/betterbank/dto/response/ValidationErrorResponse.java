package com.betterbank.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ValidationErrorResponse(
        int status,
        String message,
        List<ValidationError> errors,
        LocalDateTime timestamp
) {
    public ValidationErrorResponse(int status, String message, List<ValidationError> errors) {
        this(status, message, errors, LocalDateTime.now());
    }

    public ValidationErrorResponse(int status, String message) {
        this(status, message, List.of(), LocalDateTime.now());
    }

    public record ValidationError(String field, String message) {
    }
}
