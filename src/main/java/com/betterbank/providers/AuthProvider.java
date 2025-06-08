package com.betterbank.providers;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.RegistrationOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface AuthProvider {
    Mono<RegistrationOutcome> register(RegisterRequest registerRequest);

    Mono<JsonNode> login(LoginRequest loginRequest);
}
