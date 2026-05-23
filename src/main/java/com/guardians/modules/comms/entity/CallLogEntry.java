package com.guardians.modules.comms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "call_logs",
    indexes = @Index(columnList = "device_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CallLogEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "number", nullable = false, length = 50)
    @Builder.Default
    private String number = "";

    @Column(name = "name", nullable = false, length = 255)
    @Builder.Default
    private String name = "";

    @Column(name = "call_type", nullable = false, length = 20)
    @Builder.Default
    private String callType = "UNKNOWN";

    @Column(name = "duration_seconds", nullable = false)
    @Builder.Default
    private Integer durationSeconds = 0;

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private Long timestamp = 0L;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
