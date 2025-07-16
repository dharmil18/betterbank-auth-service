package com.betterbank.controller;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.GenericResponse;
import com.betterbank.dto.response.LoginResponse;
import com.betterbank.dto.response.LoginState;
import com.betterbank.dto.response.RegistrationOutcome;
import com.betterbank.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "BetterBank Auth Service", description = "Documentation for auth-service APIs")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/register")
    public ResponseEntity<GenericResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        LOGGER.info("Handling request for /api/auth/register");
        RegistrationOutcome registrationOutcome = authService.register(registerRequest);

        if (registrationOutcome == RegistrationOutcome.INITIATED_ASYNC_PROCESS) {
            return ResponseEntity.status(HttpStatus.CREATED).body(new GenericResponse(true, "Account creation request received. Check your email for next steps."));
        } else if (registrationOutcome == RegistrationOutcome.USER_EXISTS) {
            return ResponseEntity.status(HttpStatus.OK).body(new GenericResponse(false, "Can't create an account. Please use another email address."));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new GenericResponse(false, "Server Error/Authentication Error."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LOGGER.info("Handling request for /api/auth/login");

        LoginResponse loginResponse = authService.login(loginRequest);

        if (loginResponse.loginState() == LoginState.INVALID_CREDENTIALS) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginResponse);
        } else if (loginResponse.loginState() == LoginState.EMAIL_NOT_VERIFIED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(loginResponse);
        } else if (loginResponse.loginState() == LoginState.SERVER_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(loginResponse);
        }

        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }
}
