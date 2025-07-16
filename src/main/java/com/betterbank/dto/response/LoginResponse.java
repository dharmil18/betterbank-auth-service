package com.betterbank.dto.response;

import java.util.Optional;

public record LoginResponse(
        boolean status,
        LoginState loginState,
        String message,
        Optional<String> email,
        Optional<String> accessToken,
        Optional<String> refreshToken
) {
}
