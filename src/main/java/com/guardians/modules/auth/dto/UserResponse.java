package com.guardians.modules.auth.dto;

import com.guardians.shared.entity.User;

import java.time.Instant;

public record UserResponse(Long id, String email, String fullName, String phone,
                            User.Role role, boolean active, Instant createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(),
                u.getPhone(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
