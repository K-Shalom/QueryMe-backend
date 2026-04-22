package com.year2.queryme.model.dto;

import lombok.Data;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class ExamSessionResponse {
    private String id;
    private String examId;
    private String studentId;
    private Instant startedAt;
    private Instant submittedAt;
    private Instant expiresAt;
    private String sandboxSchema;
    private boolean isSubmitted;
    private boolean isExpired;
    private String teacherFeedback;
    private Instant serverTime;
    private Integer totalScore;
    private Integer totalMaxScore;
}