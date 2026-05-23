package com.guardians.modules.auth.service;

import com.guardians.modules.auth.dto.*;
import com.guardians.modules.auth.repository.UserRepository;
import com.guardians.shared.entity.User;
import com.guardians.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("Email already registered: " + req.email());
        }
        User.Role role = req.role() != null ? req.role() : User.Role.PARENT;

        User user = User.builder()
                .email(req.email().toLowerCase().trim())
                .fullName(req.fullName().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .role(role)
                .build();

        userRepository.save(user);
        log.info("New user registered: {} ({})", user.getEmail(), user.getRole());

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (!user.isActive()) {
            throw ApiException.forbidden("Account is deactivated");
        }

        String token = jwtService.generateToken(user);
        log.info("User logged in: {}", user.getEmail());
        return AuthResponse.of(token, user);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return UserResponse.from(user);
    }
}
