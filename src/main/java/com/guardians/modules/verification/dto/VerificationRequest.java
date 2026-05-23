package com.guardians.modules.verification.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificationRequest(
    @NotBlank String roleType,
    @NotBlank String idCardImageBase64,
    @NotBlank String selfieBase64,
    @NotBlank String birthCertImageBase64
) {}
