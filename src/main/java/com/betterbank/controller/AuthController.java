package com.betterbank.controller;

import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.GenericResponse;
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

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        LOGGER.info("Handling request for /api/auth/test");
        return ResponseEntity.ok("Auth service is working!");
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


//    @PostMapping("/register")
//    public Mono<ResponseEntity<GenericResponse>> registerUser(@Valid @RequestBody Mono<RegisterRequest> registerRequest) {
//        System.out.println();
//        log.info("Handling request for /api/auth/register");
//
//        return registerRequest.flatMap(request -> {
//            return authService.register(request).map(outcome -> {
//                switch (outcome) {
//                    case USER_EXISTS:
//                        return ResponseEntity.accepted().body(new GenericResponse("User already exists. Try with another email."));
//
//                    case INITIATED_ASYNC_PROCESS:
//                        return ResponseEntity.ok().body(new GenericResponse("Registration initiated. Please check your email to verify & complete the process."));
//
//                    case AUTH_PROVIDER_ERROR:
//                        return ResponseEntity.internalServerError().body(new GenericResponse("Authentication provider error. Please try again."));
//
//                    default:
//                        log.warn("Unknown registration outcome for {}: {}", request.getEmail(), outcome);
//                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new GenericResponse("An unexpected error occurred during registration."));
//                }
//            }).onErrorResume(error -> {
//                log.error("Registration failed for {}: {}", request.getEmail(), error.getMessage());
//                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new GenericResponse("Registration failed. Please try again.")));
//            });
//        });
//    }

//    @PostMapping("/login")
//    public Mono<GenericResponse> loginUser(@RequestBody LoginRequest user) {
//        log.info("Handling request for /api/auth/login");
//        return authService.login(user);
//    }
}
