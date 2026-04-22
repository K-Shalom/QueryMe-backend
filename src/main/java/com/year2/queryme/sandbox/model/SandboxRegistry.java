package com.year2.queryme.sandbox.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "sandbox_registry",
    indexes = {
        @Index(name = "idx_sandbox_exam_student", columnList = "exam_id, student_id"),
        @Index(name = "idx_sandbox_status_expires", columnList = "status, expires_at")
    }
)
@Data
@NoArgsConstructor
public class SandboxRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "schema_name", unique = true, nullable = false, length = 100)
    private String schemaName;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "db_user", nullable = false, length = 100)
    private String dbUser;

    @Column(name = "seed_fingerprint", length = 64)
    private String seedFingerprint;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
