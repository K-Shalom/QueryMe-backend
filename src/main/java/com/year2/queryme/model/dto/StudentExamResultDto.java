package com.year2.queryme.model.dto;

import com.year2.queryme.model.enums.VisibilityMode;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StudentExamResultDto {
    private UUID sessionId;
    private UUID examId;
    private UUID studentId;
    private VisibilityMode visibilityMode;
    private Boolean visible;
    private Integer totalScore;
    private Integer totalMaxScore;
    private List<StudentQuestionResultDto> questions;
}
