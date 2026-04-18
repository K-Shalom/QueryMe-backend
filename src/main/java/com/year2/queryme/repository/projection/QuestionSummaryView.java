package com.year2.queryme.repository.projection;

import java.util.UUID;

public interface QuestionSummaryView {
    UUID getId();
    String getPrompt();
    Integer getMarks();
}
