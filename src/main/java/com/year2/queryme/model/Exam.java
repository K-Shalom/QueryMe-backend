package com.year2.queryme.model;

import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.model.enums.VisibilityMode;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "exams",
    indexes = {
        @Index(name = "idx_exams_course", columnList = "course_id"),
        @Index(name = "idx_exams_status", columnList = "status"),
        @Index(name = "idx_exams_course_status", columnList = "course_id, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_mode", nullable = false)
    private VisibilityMode visibilityMode;

    @Column(name = "time_limit_mins")
    private Integer timeLimitMins;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "seed_sql", nullable = false, columnDefinition = "TEXT")
    private String seedSql;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = ExamStatus.DRAFT;
        if (this.maxAttempts == null) this.maxAttempts = 1;
    }
}