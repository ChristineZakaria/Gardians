package com.guardians.modules.apps.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "installed_apps",
    uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "package_name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstalledApp {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "package_name", nullable = false, length = 255)
    private String packageName;

    @Column(name = "app_name", length = 255)
    private String appName;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
