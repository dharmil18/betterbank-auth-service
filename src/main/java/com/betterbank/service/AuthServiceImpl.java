package com.betterbank.service;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.LoginResponse;
import com.betterbank.dto.response.LoginState;
import com.betterbank.dto.response.LoginStatus;
import com.betterbank.dto.response.RegistrationOutcome;
import com.betterbank.providers.AuthProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {
    private final AuthProvider authProvider;

    public AuthServiceImpl(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public RegistrationOutcome register(RegisterRequest registerRequest) {
        return authProvider.register(registerRequest);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        LoginStatus loginStatus = authProvider.login(loginRequest);

        if (loginStatus.loginState() == LoginState.INVALID_CREDENTIALS) {
            return new LoginResponse(false, LoginState.INVALID_CREDENTIALS, "Invalid credentials provided.", Optional.empty(), Optional.empty(), Optional.empty());
        } else if (loginStatus.loginState() == LoginState.EMAIL_NOT_VERIFIED) {
            return new LoginResponse(false, LoginState.EMAIL_NOT_VERIFIED, "Email not verified. Please check your email for verification link or request a new one.", Optional.empty(), Optional.empty(), Optional.empty());
        } else if (loginStatus.loginState() == LoginState.SERVER_ERROR) {
            return new LoginResponse(false, LoginState.SERVER_ERROR, "An error occurred while processing your request. Please try again later.", Optional.empty(), Optional.empty(), Optional.empty());
        }

        return new LoginResponse(true, LoginState.LOGGED_IN,
                "Login successful.", Optional.of(loginRequest.email()),
                loginStatus.accessToken(), loginStatus.refreshToken());
    }
}
