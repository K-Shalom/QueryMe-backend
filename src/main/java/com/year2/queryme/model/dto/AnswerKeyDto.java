package com.year2.queryme.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerKeyDto {
    private UUID id;
    private UUID questionId;
    private String expectedColumns;
    private String expectedRows;
}