package com.guardians.modules.device.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LinkByBirthCertRequest(
    @NotBlank @Email String parentEmail,
    @NotBlank       String parentPassword,
    @NotBlank       String birthCertImageBase64,
                    String deviceInfo,
                    String deviceName,
                    String fcmToken,
                    String childGender,
                    String childName,
                    String childEmail,
                    String childPassword
) {}
