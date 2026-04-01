package com.year2.queryme.model.dto;

import lombok.Data;

@Data
public class StartSessionRequest {
    private String examId;
    private String studentId;
}