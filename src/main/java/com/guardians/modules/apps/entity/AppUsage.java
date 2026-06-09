package com.guardians.modules.apps.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Entity
@Table(name = "app_usage",
    uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "package_name", "date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUsage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "package_name", nullable = false, length = 255)
    private String packageName;

    @Column(name = "app_name", length = 255)
    private String appName;

    @Column(name = "usage_millis", nullable = false)
    @Builder.Default
    private Long usageMillis = 0L;

    @Column(name = "date", nullable = false)
    @Builder.Default
    private LocalDate date = LocalDate.now(ZoneOffset.UTC);

    @Column(name = "reported_at", nullable = false)
    @Builder.Default
    private Instant reportedAt = Instant.now();
}
