package com.betterbank.exception;

import com.betterbank.dto.response.LoginError;
import com.betterbank.dto.response.ValidationErrorResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        LOGGER.error("Validation Error: {}", ex.getMessage());
        List<ValidationErrorResponse.ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationErrorResponse.ValidationError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        ValidationErrorResponse validationErrorResponse = new ValidationErrorResponse(400, "Validation Failed", errors);
        return ResponseEntity.badRequest().body(validationErrorResponse);
    }

    @ExceptionHandler(FeignException.Unauthorized.class)
    public ResponseEntity<LoginError> handleFeignUnauthorized(FeignException.Unauthorized ex) {
        LOGGER.error("Feign Unauthorized Error: {}", ex.getMessage());
        LoginError error = new LoginError(false, "Unauthorized");
        return ResponseEntity.status(401).body(error);
    }

    @ExceptionHandler(jakarta.ws.rs.ProcessingException.class)
    public ResponseEntity<LoginError> handleKeycloakProcessingException(jakarta.ws.rs.ProcessingException ex) {
        LOGGER.error("Keycloak Processing Error: {}", ex.getMessage());
        return ResponseEntity.status(503).body(new LoginError(false, "Authentication provider error"));
    }

    @ExceptionHandler(jakarta.ws.rs.WebApplicationException.class)
    public ResponseEntity<LoginError> handleKeycloakWebAppException(jakarta.ws.rs.WebApplicationException ex) {
        LOGGER.error("Keycloak WebApplication Error: {}", ex.getMessage());
        int status = ex.getResponse() != null ? ex.getResponse().getStatus() : 500;
        return ResponseEntity.status(status).body(new LoginError(false, "Authentication provider error"));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<LoginError> handleNullPointerException(NullPointerException ex) {
        LOGGER.error("Null Pointer Exception: {}", ex.getMessage());
        return ResponseEntity.status(500).body(new LoginError(false, "Internal server error"));
    }

    @ExceptionHandler(IndexOutOfBoundsException.class)
    public ResponseEntity<LoginError> handleIndexOutOfBoundsException(IndexOutOfBoundsException ex) {
        LOGGER.error("Index Out Of Bounds Exception: {}", ex.getMessage());
        return ResponseEntity.status(500).body(new LoginError(false, "Internal server error"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<LoginError> handleIllegalArgumentException(IllegalArgumentException ex) {
        LOGGER.error("Illegal Argument Exception: {}", ex.getMessage());
        return ResponseEntity.status(400).body(new LoginError(false, "Bad request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<LoginError> handleGenericException(Exception ex) {
        LOGGER.error("Unexpected Error: {}", ex.getMessage());
        return ResponseEntity.status(500).body(new LoginError(false, "Internal server error"));
    }

}
