package com.year2.queryme.service;

import com.year2.queryme.model.Exam;
import com.year2.queryme.model.ExamSession;
import com.year2.queryme.model.dto.*;
import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.model.mapper.ExamSessionMapper;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.ExamSessionRepository;
import com.year2.queryme.sandbox.service.SandboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {

    private final ExamSessionRepository sessionRepository;
    private final ExamRepository examRepository;
    private final SandboxService sandboxService;

    @Override
    @Transactional
    public ExamSessionResponse startSession(StartSessionRequest request) {

        // Rule 1 — exam must exist and be PUBLISHED
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("Exam is not published");
        }

        // Rule 2 — student cannot start the same exam twice
        if (sessionRepository.existsByExamIdAndStudentId(
                request.getExamId(), request.getStudentId())) {
            throw new RuntimeException("Student already has a session for this exam");
        }

        // Calculate expiry time from exam timer
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = exam.getTimeLimitMins() != null
                ? now.plusMinutes(exam.getTimeLimitMins())
                : null;

        String sandboxSchema = sandboxService.provisionSandbox(
                UUID.fromString(request.getExamId()),
                UUID.fromString(request.getStudentId()),
                exam.getSeedSql());

        ExamSession session = ExamSession.builder()
                .examId(request.getExamId())
                .studentId(request.getStudentId())
                .startedAt(now)
                .expiresAt(expiresAt)
                .sandboxSchema(sandboxSchema)
                .build();

        return ExamSessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional
    public ExamSessionResponse submitSession(String sessionId) {
        ExamSession session = findById(sessionId);

        // Rule — cannot submit twice
        if (session.getSubmittedAt() != null) {
            throw new RuntimeException("Session already submitted");
        }

        // Rule — cannot submit after expiry
        if (session.getExpiresAt() != null
                && LocalDateTime.now().isAfter(session.getExpiresAt())) {
            throw new RuntimeException("Session has expired");
        }

        session.setSubmittedAt(LocalDateTime.now());
        ExamSession savedSession = sessionRepository.save(session);
        sandboxService.teardownSandbox(
                UUID.fromString(savedSession.getExamId()),
                UUID.fromString(savedSession.getStudentId()));
        return ExamSessionMapper.toResponse(savedSession);
    }

    @Override
    public ExamSessionResponse getSessionById(String sessionId) {
        return ExamSessionMapper.toResponse(findById(sessionId));
    }

    @Override
    public List<ExamSessionResponse> getSessionsByExam(String examId) {
        return sessionRepository.findByExamId(examId)
                .stream().map(ExamSessionMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExamSessionResponse> getSessionsByStudent(String studentId) {
        return sessionRepository.findByStudentId(studentId)
                .stream().map(ExamSessionMapper::toResponse).collect(Collectors.toList());
    }

    private ExamSession findById(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }
}

