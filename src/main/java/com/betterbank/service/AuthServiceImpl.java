package com.betterbank.service;

import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.RegistrationOutcome;
import com.betterbank.providers.AuthProvider;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final AuthProvider authProvider;

    public AuthServiceImpl(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public RegistrationOutcome register(RegisterRequest registerRequest) {
        return authProvider.register(registerRequest);
    }

}
