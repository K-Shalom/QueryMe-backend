package com.year2.queryme.service;

import com.year2.queryme.model.Result;
import com.year2.queryme.model.Question;
import com.year2.queryme.model.Submission;
import com.year2.queryme.model.dto.StudentExamResultDto;
import com.year2.queryme.model.dto.TeacherDashboardRowDto;

import java.util.List;
import java.util.UUID;

public interface ResultService {
    StudentExamResultDto getResultsForStudent(UUID sessionId);
    void processNewSubmission(UUID submissionId);
    void processNewSubmission(Submission submission, Question question);
    List<TeacherDashboardRowDto> getResultsForTeacher(UUID examId);
    Result saveQueryResult(UUID submissionId, Integer score, Boolean isCorrect);
    Result saveQueryResult(Submission submission, Question question, Integer score, Boolean isCorrect);
}
