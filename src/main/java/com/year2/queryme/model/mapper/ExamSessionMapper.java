package com.year2.queryme.model.mapper;

import com.year2.queryme.model.ExamSession;
import com.year2.queryme.model.dto.ExamSessionResponse;
import java.time.LocalDateTime;

public class ExamSessionMapper {

    public static ExamSessionResponse toResponse(ExamSession session) {
        ExamSessionResponse res = new ExamSessionResponse();
        res.setId(session.getId());
        res.setExamId(session.getExamId());
        res.setStudentId(session.getStudentId());
        res.setStartedAt(session.getStartedAt());
        res.setSubmittedAt(session.getSubmittedAt());
        res.setExpiresAt(session.getExpiresAt());
        res.setSandboxSchema(session.getSandboxSchema());
        res.setSubmitted(session.getSubmittedAt() != null);
        res.setExpired(session.getExpiresAt() != null
                && LocalDateTime.now().isAfter(session.getExpiresAt()));
        res.setTeacherFeedback(session.getTeacherFeedback());
        res.setServerTime(LocalDateTime.now());
        return res;
    }
}