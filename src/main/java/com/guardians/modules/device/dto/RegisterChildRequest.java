package com.guardians.modules.device.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterChildRequest(
        @NotBlank @Email   String childEmail,
        @NotBlank @Size(min = 6) String childPassword,
        @NotBlank          String childName,
        String             deviceName,
        String             birthCertImageBase64,
        String             childGender
) {}
