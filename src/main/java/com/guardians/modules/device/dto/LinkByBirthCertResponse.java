package com.guardians.modules.device.dto;

public record LinkByBirthCertResponse(
    boolean success,
    String  message,
    String  deviceId,
    Long    parentId,
    String  parentName,
    String  token
) {}
