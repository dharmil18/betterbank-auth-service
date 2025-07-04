package com.betterbank.providers;

import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.RegistrationOutcome;

public interface AuthProvider {
    RegistrationOutcome register(RegisterRequest registerRequest);
}
