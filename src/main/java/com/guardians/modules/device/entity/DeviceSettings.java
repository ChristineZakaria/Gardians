package com.guardians.modules.device.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "device_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceSettings {

    @Id
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "screen_time_limit_minutes", nullable = false)
    @Builder.Default
    private Integer screenTimeLimitMinutes = 0;

    @Column(name = "image_detection_enabled", nullable = false)
    @Builder.Default
    private Boolean imageDetectionEnabled = true;

    @Column(name = "video_detection_enabled", nullable = false)
    @Builder.Default
    private Boolean videoDetectionEnabled = true;

    @Column(name = "content_scan_enabled", nullable = false)
    @Builder.Default
    private Boolean contentScanEnabled = true;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
