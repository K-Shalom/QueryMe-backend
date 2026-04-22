package com.year2.queryme.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface TeacherDashboardSubmissionView {
    UUID getStudentId();
    UUID getSessionId();
    UUID getQuestionId();
    Integer getScore();
    Boolean getIsCorrect();
    String getSubmittedQuery();
    Instant getSubmittedAt();
    String getTeacherFeedback();
}
