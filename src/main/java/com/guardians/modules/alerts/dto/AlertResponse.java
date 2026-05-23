package com.guardians.modules.alerts.dto;

import com.guardians.shared.entity.AlertEntity;

import java.time.Instant;
import java.util.Map;

public record AlertResponse(
    Long id,
    Long parentId,
    Long senderDeviceId,
    String senderDeviceName,
    AlertEntity.AlertType type,
    String category,
    String title,
    String message,
    AlertEntity.Severity severity,
    boolean read,
    Map<String, Object> metadata,
    Instant createdAt
) {
    public static AlertResponse from(AlertEntity a) {
        // Use detectedCategory from metadata (set by ContentAlertReporter) if available
        String category = null;
        if (a.getMetadata() != null) {
            Object cat = a.getMetadata().get("detectedCategory");
            if (cat instanceof String s) category = s;
        }
        // Fall back to type name in lowercase for non-content-detection alerts
        if (category == null) category = a.getType().name().toLowerCase();

        return new AlertResponse(
            a.getId(),
            a.getParent().getId(),
            a.getSenderDevice() != null ? a.getSenderDevice().getId() : null,
            a.getSenderDevice() != null ? a.getSenderDevice().getDeviceName() : null,
            a.getType(), category, a.getTitle(), a.getMessage(), a.getSeverity(),
            a.isRead(), a.getMetadata(), a.getCreatedAt()
        );
    }
}
