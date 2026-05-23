package com.guardians.modules.alerts.dto;

import jakarta.validation.constraints.NotBlank;

public record ContentDetectionRequest(
    @NotBlank String deviceId,
    String childName,
    @NotBlank String appName,
    @NotBlank String detectedCategory,
    double confidence,
    String timestamp,
    String detail
) {}
