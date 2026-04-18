package com.year2.queryme.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "questions",
    indexes = {
        @Index(name = "idx_questions_exam_order", columnList = "exam_id, order_index"),
        @Index(name = "idx_questions_exam", columnList = "exam_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "reference_query", nullable = false, columnDefinition = "TEXT")
    private String referenceQuery;

    @Column(nullable = false)
    private Integer marks;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "order_sensitive")
    @Builder.Default
    private Boolean orderSensitive = false;

    @Column(name = "partial_marks")
    @Builder.Default
    private Boolean partialMarks = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}