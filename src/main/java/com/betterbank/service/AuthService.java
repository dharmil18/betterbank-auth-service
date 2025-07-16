package com.betterbank.service;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.LoginResponse;
import com.betterbank.dto.response.RegistrationOutcome;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    public RegistrationOutcome register(RegisterRequest registerRequest);

    public LoginResponse login(LoginRequest loginRequest);

}
