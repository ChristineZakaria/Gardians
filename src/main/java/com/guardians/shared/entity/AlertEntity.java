package com.guardians.shared.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_device_id")
    private Device senderDevice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum AlertType {
        GEOFENCE_EXIT, GEOFENCE_ENTER,
        INAPPROPRIATE_IMAGE, INAPPROPRIATE_VIDEO,
        UNSAFE_URL, INAPPROPRIATE_TEXT,
        APP_BLOCKED, DEVICE_BLOCKED,
        DEVICE_OFFLINE, LOW_BATTERY,
        SCREEN_TIME_EXCEEDED, SELF_HARM, GENERAL, SOS
    }

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
}
