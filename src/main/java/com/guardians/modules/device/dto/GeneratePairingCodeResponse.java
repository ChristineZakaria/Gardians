package com.guardians.modules.device.dto;

import java.time.Instant;

public record GeneratePairingCodeResponse(
    String code,
    Instant expiresAt,
    String message
) {}
