package com.year2.queryme.model.dto;

import com.year2.queryme.model.enums.VisibilityMode;
import lombok.Data;

@Data
public class CreateExamRequest {
    private String courseId;
    private String title;
    private String description;
    private VisibilityMode visibilityMode;  // IMMEDIATE | END_OF_EXAM | NEVER
    private Integer timeLimitMins;          // optional — null means no limit
    private Integer maxAttempts;            // default 1
    private String seedSql;                 // required — SQL for sandbox seeding
}