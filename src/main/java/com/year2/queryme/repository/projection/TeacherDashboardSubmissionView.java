package com.year2.queryme.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TeacherDashboardSubmissionView {
    UUID getStudentId();
    UUID getSessionId();
    UUID getQuestionId();
    Integer getScore();
    Boolean getIsCorrect();
    String getSubmittedQuery();
    LocalDateTime getSubmittedAt();
}
