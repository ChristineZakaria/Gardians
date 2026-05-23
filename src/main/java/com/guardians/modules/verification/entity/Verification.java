package com.guardians.modules.verification.entity;

import com.guardians.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "verifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Verification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_type", length = 20)
    private String roleType;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "face_match_score")
    private Double faceMatchScore;

    @Column(name = "name_match")
    private Boolean nameMatch;

    @Column(name = "child_name", length = 150)
    private String childName;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
