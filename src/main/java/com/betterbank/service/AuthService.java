package com.betterbank.service;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.RegistrationOutcome;
import com.betterbank.providers.AuthProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {
    private final AuthProvider authProvider;

    public AuthService(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public Mono<RegistrationOutcome> register(RegisterRequest registerRequest) {
        return authProvider.register(registerRequest);
    }
}
