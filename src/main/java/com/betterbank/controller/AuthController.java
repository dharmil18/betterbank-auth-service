package com.betterbank.controller;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.GenericResponse;
import com.betterbank.dto.response.RegistrationOutcome;
import com.betterbank.providers.KeycloakAuthProvider;
import com.betterbank.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Tag(name = "BetterBank Auth Service", description = "Documentation for auth-service APIs")
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("Handling request for /api/auth/test");
        return ResponseEntity.ok("Auth service is working!");
    }

    @PostMapping("/register")
    public Mono<RegistrationOutcome> registerUser(@RequestBody RegisterRequest user) {
        log.info("Handling request for /api/auth/register");
        return authService.register(user);
    }

    @PostMapping("/login")
    public Mono<GenericResponse> loginUser(@RequestBody LoginRequest user) {
        log.info("Handling request for /api/auth/login");
        return authService.login(user);
    }
}
