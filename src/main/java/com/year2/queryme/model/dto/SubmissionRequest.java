package com.year2.queryme.model.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class SubmissionRequest {
    private UUID sessionId;
    private UUID examId;
    private UUID questionId;
    private UUID studentId;
    private String query;
}
