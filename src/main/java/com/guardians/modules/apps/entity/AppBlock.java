package com.guardians.modules.apps.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "app_blocks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "package_name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppBlock {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "package_name", nullable = false, length = 255)
    private String packageName;

    @Column(name = "blocked_at", nullable = false)
    @Builder.Default
    private Instant blockedAt = Instant.now();
}
