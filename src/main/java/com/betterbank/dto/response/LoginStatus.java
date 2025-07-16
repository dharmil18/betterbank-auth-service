package com.betterbank.dto.response;

import java.util.Optional;

public record LoginStatus(
        LoginState loginState,
        Optional<String> accessToken,
        Optional<String> refreshToken
) {
}
