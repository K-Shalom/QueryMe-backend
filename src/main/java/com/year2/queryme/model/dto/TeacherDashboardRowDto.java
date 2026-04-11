package com.year2.queryme.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TeacherDashboardRowDto {
    private UUID studentId;
    private String studentName;
    private UUID sessionId;
    private UUID questionId;
    private String questionPrompt;
    private Integer score;
    private Integer maxScore;
    private Boolean isCorrect;
    private String submittedQuery;
    private LocalDateTime submittedAt;
}
