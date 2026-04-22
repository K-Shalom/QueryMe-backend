package com.year2.queryme.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "submissions",
    indexes = {
        @Index(name = "idx_submissions_exam_submitted_at", columnList = "exam_id, submitted_at"),
        @Index(name = "idx_submissions_session_submitted_at", columnList = "session_id, submitted_at"),
        @Index(name = "idx_submissions_student_exam", columnList = "student_id, exam_id"),
        @Index(name = "idx_submissions_question", columnList = "question_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "submitted_query", nullable = false, columnDefinition = "TEXT")
    private String submittedQuery;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(nullable = false)
    private Integer score;
    
    @Column(name = "execution_error", columnDefinition = "TEXT")
    private String executionError;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_columns", columnDefinition = "jsonb")
    private String resultColumns;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_rows", columnDefinition = "jsonb")
    private String resultRows;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private Instant submittedAt;
}
