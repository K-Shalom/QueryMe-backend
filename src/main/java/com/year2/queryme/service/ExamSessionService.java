package com.year2.queryme.service;
import com.year2.queryme.model.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ExamSessionService {
    ExamSessionResponse startSession(StartSessionRequest request);
    ExamSessionResponse submitSession(String sessionId);
    ExamSessionResponse getSessionById(String sessionId);
    Page<ExamSessionResponse> getSessionsByExam(String examId, Pageable pageable);
    Page<ExamSessionResponse> getSessionsByStudent(String studentId, Pageable pageable);
}