package com.guardians.modules.device.dto;

public record PairingResultResponse(
    boolean success,
    String message,
    Long parentId,
    String parentName,
    DeviceResponse device
) {}
