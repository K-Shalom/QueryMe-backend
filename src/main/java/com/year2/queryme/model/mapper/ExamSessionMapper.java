package com.year2.queryme.model.mapper;

import com.year2.queryme.model.ExamSession;
import com.year2.queryme.model.dto.ExamSessionResponse;
import java.time.Instant;

public class ExamSessionMapper {

    public static ExamSessionResponse toResponse(ExamSession session) {
        if (session == null) return null;
        ExamSessionResponse res = new ExamSessionResponse();
        res.setId(session.getId().toString());
        res.setExamId(session.getExamId().toString());
        res.setStudentId(session.getStudentId());
        res.setStartedAt(session.getStartedAt());
        res.setSubmittedAt(session.getSubmittedAt());
        res.setExpiresAt(session.getExpiresAt());
        res.setSandboxSchema(session.getSandboxSchema());
        res.setSubmitted(session.getSubmittedAt() != null);
        res.setExpired(session.getExpiresAt() != null
                && Instant.now().isAfter(session.getExpiresAt()));
        res.setTeacherFeedback(session.getTeacherFeedback());
        res.setServerTime(Instant.now());
        return res;
    }
}