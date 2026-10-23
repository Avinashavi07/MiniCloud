package com.minicloud.auth.dto;

import com.minicloud.auth.entity.Role;
import com.minicloud.auth.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String displayName,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
