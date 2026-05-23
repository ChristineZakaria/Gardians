package com.guardians.modules.comms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sms_logs",
    indexes = @Index(columnList = "device_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmsLogEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "address", nullable = false, length = 255)
    @Builder.Default
    private String address = "";

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String body = "";

    @Column(name = "sms_type", nullable = false, length = 20)
    @Builder.Default
    private String smsType = "OTHER";

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private Long timestamp = 0L;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
