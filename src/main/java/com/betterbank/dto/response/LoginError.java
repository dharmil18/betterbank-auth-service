package com.betterbank.dto.response;

public record LoginError(
        boolean status,
        String errorDescription
) {
}
