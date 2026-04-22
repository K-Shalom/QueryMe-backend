package com.year2.queryme.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExamSessionResponse {
    private String id;
    private String examId;
    private String studentId;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime expiresAt;
    private String sandboxSchema;
    private boolean isSubmitted;
    private boolean isExpired;
    private String teacherFeedback;
    private LocalDateTime serverTime;
    private Integer totalScore;
    private Integer totalMaxScore;
}