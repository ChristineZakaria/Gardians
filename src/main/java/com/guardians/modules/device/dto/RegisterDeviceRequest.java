package com.guardians.modules.device.dto;

import com.guardians.shared.entity.Device;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
    @NotBlank @Size(max = 100) String deviceId,
    @Size(max = 150) String deviceName,
    Device.DeviceType type,
    String fcmToken
) {}
