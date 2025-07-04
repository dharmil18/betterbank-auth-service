package com.betterbank.dto.response;

public record GenericResponse(
        boolean successStatus,
        String message
) {
}
