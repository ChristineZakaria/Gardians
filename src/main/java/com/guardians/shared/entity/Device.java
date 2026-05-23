package com.guardians.shared.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 100)
    private String deviceId;

    @Column(name = "device_name", length = 150)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeviceType type = DeviceType.CHILD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_parent_id")
    private User linkedParent;

    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "blocked", nullable = false)
    @Builder.Default
    private boolean blocked = false;

    @Column(name = "battery_level")
    @Builder.Default
    private int batteryLevel = 0;

    @Column(name = "is_charging")
    @Builder.Default
    private boolean charging = false;

    @Column(name = "current_app", length = 150)
    private String currentApp;

    public enum DeviceType { PARENT, CHILD }
}
