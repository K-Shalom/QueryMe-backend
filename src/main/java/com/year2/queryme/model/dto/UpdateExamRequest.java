package com.year2.queryme.model.dto;

import com.year2.queryme.model.enums.VisibilityMode;
import lombok.Data;

@Data
public class UpdateExamRequest {
    private String title;
    private String description;
    private VisibilityMode visibilityMode;
    private Integer timeLimitMins;
    private Integer maxAttempts;
    private String seedSql;
}