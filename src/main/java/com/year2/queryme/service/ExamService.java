package com.year2.queryme.service;

import com.year2.queryme.model.dto.*;
import java.util.List;

public interface ExamService {
    ExamResponse createExam(CreateExamRequest request);
    ExamResponse getExamById(String examId);
    List<ExamResponse> getExamsByCourse(String courseId);
    List<ExamResponse> getPublishedExams();
    ExamResponse updateExam(String examId, UpdateExamRequest request);
    ExamResponse publishExam(String examId);
    ExamResponse unpublishExam(String examId);
    ExamResponse closeExam(String examId);
    void deleteExam(String examId);
}