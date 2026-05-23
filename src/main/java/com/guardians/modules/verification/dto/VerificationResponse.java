package com.guardians.modules.verification.dto;

import com.guardians.modules.verification.entity.Verification;

public record VerificationResponse(
    Long    id,
    String  status,
    String  roleType,
    Double  faceMatchScore,
    Boolean nameMatch,
    String  rejectReason,
    String  createdAt
) {
    public static VerificationResponse from(Verification v) {
        return new VerificationResponse(
            v.getId(),
            v.getStatus(),
            v.getRoleType(),
            v.getFaceMatchScore(),
            v.getNameMatch(),
            v.getRejectReason(),
            v.getCreatedAt() != null ? v.getCreatedAt().toString() : null
        );
    }
}
