package com.year2.queryme.model.dto;

import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.model.enums.VisibilityMode;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class ExamResponse {
    private String id;
    private String courseId;
    private String title;
    private String description;
    private ExamStatus status;
    private VisibilityMode visibilityMode;
    private Integer timeLimitMins;
    private Integer maxAttempts;
    private Integer questionCount;
    private Integer questionsCount;
    private String seedSql;
    private String courseName;
    private Instant createdAt;
    private Instant publishedAt;
}
