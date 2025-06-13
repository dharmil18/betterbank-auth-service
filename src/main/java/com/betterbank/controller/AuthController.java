package com.betterbank.controller;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.GenericResponse;
import com.betterbank.providers.AuthProvider;
import com.betterbank.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Tag(name = "BetterBank Auth Service", description = "Documentation for auth-service APIs")
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final AuthProvider authProvider;

    public AuthController(AuthService authService, AuthProvider authProvider) {
        this.authService = authService;
        this.authProvider = authProvider;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("Handling request for /api/auth/test");
        return ResponseEntity.ok("Auth service is working!");
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<GenericResponse>> registerUser(@Valid @RequestBody Mono<RegisterRequest> registerRequest) {
        System.out.println();
        log.info("Handling request for /api/auth/register");

        return registerRequest.flatMap(request -> {
            return authProvider.register(request)
                    .map(outcome -> {
                        switch (outcome) {
                            case USER_EXISTS:
                                return ResponseEntity.accepted().body(new GenericResponse("User already exists. Try with another email."));

                            case INITIATED_ASYNC_PROCESS:
                                return ResponseEntity
                                        .accepted()
                                        .body(new GenericResponse("Registration initiated. Please check your email to verify & complete the process."));

                            case AUTH_PROVIDER_ERROR:
                                return ResponseEntity.internalServerError().body(new GenericResponse("Authentication provider error. Please try again."));

                            default:
                                log.warn("Unknown registration outcome for {}: {}", request.getEmail(), outcome);
                                return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new GenericResponse("An unexpected error occurred during registration."));
                        }
                    })
                    .onErrorResume(error -> {
                        log.error("Registration failed for {}: {}", request.getEmail(), error.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new GenericResponse("Registration failed. Please try again.")));
                    });
        });
    }

    @PostMapping("/login")
    public Mono<GenericResponse> loginUser(@RequestBody LoginRequest user) {
        log.info("Handling request for /api/auth/login");
        return authService.login(user);
    }
}
