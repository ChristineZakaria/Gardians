package com.guardians.modules.alerts.dto;

import com.guardians.shared.entity.AlertEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record SendAlertRequest(
    @NotNull AlertEntity.AlertType type,
    @NotBlank @Size(max = 255) String title,
    String message,
    AlertEntity.Severity severity,
    @NotBlank String deviceId,
    Map<String, Object> metadata
) {}
