package com.year2.queryme.model.mapper;

import com.year2.queryme.model.Exam;
import com.year2.queryme.model.dto.ExamResponse;

public class ExamMapper {

    public static ExamResponse toResponse(Exam exam) {
        ExamResponse res = new ExamResponse();
        res.setId(exam.getId());
        res.setCourseId(exam.getCourseId());
        res.setTitle(exam.getTitle());
        res.setDescription(exam.getDescription());
        res.setStatus(exam.getStatus());
        res.setVisibilityMode(exam.getVisibilityMode());
        res.setTimeLimitMins(exam.getTimeLimitMins());
        res.setMaxAttempts(exam.getMaxAttempts());
        res.setSeedSql(exam.getSeedSql());
        res.setCreatedAt(exam.getCreatedAt());
        res.setPublishedAt(exam.getPublishedAt());
        return res;
    }
}