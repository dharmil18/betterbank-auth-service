package com.betterbank.providers;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.LoginResponse;
import com.betterbank.dto.response.LoginStatus;
import com.betterbank.dto.response.RegistrationOutcome;

public interface AuthProvider {
    RegistrationOutcome register(RegisterRequest registerRequest);

    LoginStatus login(LoginRequest loginRequest);
}
