package com.betterbank.service;

import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.RegistrationOutcome;

public interface AuthService {
    public RegistrationOutcome register(RegisterRequest registerRequest);
}
