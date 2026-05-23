package com.guardians.modules.auth.dto;

import com.guardians.shared.entity.User;

public record AuthResponse(
    String token,
    String tokenType,
    Long userId,
    String email,
    String fullName,
    User.Role role
) {
    public static AuthResponse of(String token, com.guardians.shared.entity.User user) {
        return new AuthResponse(token, "Bearer", user.getId(),
                user.getEmail(), user.getFullName(), user.getRole());
    }
}
