package com.year2.queryme.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class StudentQuestionResultDto {
    private UUID questionId;
    private String prompt;
    private String submittedQuery;
    private Integer score;
    private Integer maxScore;
    private Boolean isCorrect;
    private LocalDateTime submittedAt;
    private List<String> resultColumns;
    private List<Map<String, Object>> resultRows;
}
