package com.guardians.modules.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsePairingCodeRequest(
    @NotBlank @Size(min = 6, max = 6) String code,
    String deviceId,   // optional — server generates UUID if absent (e.g. app reinstall)
    String deviceName,
    String fcmToken
) {}
