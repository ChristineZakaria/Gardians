package com.guardians.modules.apps.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "app_limits",
    uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "package_name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppLimit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "package_name", nullable = false, length = 255)
    private String packageName;

    @Column(name = "limit_minutes", nullable = false)
    private int limitMinutes;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
