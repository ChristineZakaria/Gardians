package com.guardians.modules.device.dto;

import com.guardians.shared.entity.Device;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record DeviceResponse(
    Long id,
    String deviceId,
    String deviceName,
    Device.DeviceType type,
    Long ownerUserId,
    Long linkedParentId,
    String fcmToken,
    boolean active,
    boolean blocked,
    boolean online,
    Instant lastSeen,
    Instant createdAt,
    int batteryLevel,
    boolean isCharging,
    String currentApp
) {
    private static final long ONLINE_THRESHOLD_MINUTES = 2;

    public static DeviceResponse from(Device d) {
        boolean online = d.getLastSeen() != null &&
            d.getLastSeen().isAfter(Instant.now().minus(ONLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES));
        return new DeviceResponse(
            d.getId(), d.getDeviceId(), d.getDeviceName(), d.getType(),
            d.getOwner().getId(),
            d.getLinkedParent() != null ? d.getLinkedParent().getId() : null,
            d.getFcmToken(), d.isActive(), d.isBlocked(), online, d.getLastSeen(), d.getCreatedAt(),
            d.getBatteryLevel(), d.isCharging(), d.getCurrentApp()
        );
    }
}
