package com.year2.queryme.model.dto;

import com.year2.queryme.model.enums.QuestionResultStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
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
    private QuestionResultStatus status;
    private Boolean isCorrect;
    private Instant submittedAt;
    private List<String> resultColumns;
    private List<Map<String, Object>> resultRows;
}
