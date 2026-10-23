package com.minicloud.auth.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {
}
