package com.year2.queryme.model;
import jakarta.persistence.*;
        import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "results",
    indexes = {
        @Index(name = "idx_results_submission", columnList = "submission_id"),
        @Index(name = "idx_results_session", columnList = "session_id"),
        @Index(name = "idx_results_exam_question", columnList = "exam_id, question_id")
    }
)
@Data
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId; // From Group G [cite: 61]

    @Column(name = "question_id", nullable = false)
    private UUID questionId; // From Group I [cite: 61]

    @Column(name = "session_id", nullable = false)
    private UUID sessionId; // Links to the student's attempt [cite: 61]

    @Column(name = "exam_id")
    private UUID examId;

    private Integer score;
    private Integer maxScore;
    private Boolean isCorrect;
    private LocalDateTime gradedAt;
}