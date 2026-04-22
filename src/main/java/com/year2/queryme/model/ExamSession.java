package com.year2.queryme.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "exam_sessions",
    indexes = {
        @Index(name = "idx_exam_sessions_exam", columnList = "exam_id"),
        @Index(name = "idx_exam_sessions_student", columnList = "student_id"),
        @Index(name = "idx_exam_sessions_exam_student_started", columnList = "exam_id, student_id, started_at"),
        @Index(name = "idx_exam_sessions_submitted_expires", columnList = "submitted_at, expires_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "exam_id", nullable = false)
    private String examId;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "sandbox_schema", length = 100)
    private String sandboxSchema;

    @Column(name = "teacher_feedback", length = 2000)
    private String teacherFeedback;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;
}