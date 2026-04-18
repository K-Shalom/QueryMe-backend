package com.year2.queryme.service;

import com.year2.queryme.model.Exam;
import com.year2.queryme.model.ExamSession;
import com.year2.queryme.model.Student;
import com.year2.queryme.model.dto.*;
import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.model.enums.UserTypes;
import com.year2.queryme.model.mapper.ExamSessionMapper;
import com.year2.queryme.repository.CourseEnrollmentRepository;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.ExamSessionRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.sandbox.service.SandboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {

    private final ExamSessionRepository sessionRepository;
    private final ExamRepository examRepository;
    private final SandboxService sandboxService;
    private final CurrentUserService currentUserService;
    private final StudentRepository studentRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    @Override
    @Transactional
    public ExamSessionResponse startSession(StartSessionRequest request) {

        // Rule 1 — exam must exist and be PUBLISHED
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("Exam is not published");
        }

        validateStudentOwnershipAndAssignment(exam, request.getStudentId());

        List<ExamSession> existingSessions = sessionRepository
                .findByExamIdAndStudentIdOrderByStartedAtDesc(request.getExamId(), request.getStudentId());

        for (ExamSession existingSession : existingSessions) {
            if (isExpiredAndOpen(existingSession)) {
                autoSubmit(existingSession);
            }
        }

        boolean hasActiveSession = existingSessions.stream()
                .anyMatch(session -> session.getSubmittedAt() == null && !isExpired(session));
        if (hasActiveSession) {
            throw new RuntimeException("Student already has an active session for this exam");
        }

        if (existingSessions.size() >= exam.getMaxAttempts()) {
            throw new RuntimeException("Maximum attempts reached for this exam");
        }

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
        assertCurrentUserCanAccessSession(session);

        if (session.getSubmittedAt() != null) {
            throw new RuntimeException("Session already submitted");
        }

        if (isExpired(session)) {
            return ExamSessionMapper.toResponse(autoSubmit(session));
        }

        session.setSubmittedAt(LocalDateTime.now());
        return ExamSessionMapper.toResponse(autoSubmit(session));
    }

    @Override
    public ExamSessionResponse getSessionById(String sessionId) {
        ExamSession session = findById(sessionId);
        assertCurrentUserCanAccessSession(session);
        if (isExpiredAndOpen(session)) {
            session = autoSubmit(session);
        }
        return ExamSessionMapper.toResponse(session);
    }

    @Override
    public Page<ExamSessionResponse> getSessionsByExam(String examId, Pageable pageable) {
        assertCurrentUserCanViewExamSessions();
        return sessionRepository.findByExamId(examId, pageable)
                .map(session -> isExpiredAndOpen(session) ? autoSubmit(session) : session)
                .map(ExamSessionMapper::toResponse);
    }

    @Override
    public Page<ExamSessionResponse> getSessionsByStudent(String studentId, Pageable pageable) {
        assertCurrentUserCanAccessStudentId(studentId);
        return sessionRepository.findByStudentId(studentId, pageable)
                .map(session -> isExpiredAndOpen(session) ? autoSubmit(session) : session)
                .map(ExamSessionMapper::toResponse);
    }

    private ExamSession findById(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }

    private void validateStudentOwnershipAndAssignment(Exam exam, String studentUserId) {
        assertCurrentUserCanAccessStudentId(studentUserId);

        Student student = studentRepository.findByUser_Id(UUID.fromString(studentUserId))
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        if (student.getCourse() != null && Objects.equals(student.getCourse().getId().toString(), exam.getCourseId())) {
            return;
        }

        Set<String> enrolledCourseIds = courseEnrollmentRepository.findByStudentId(student.getId())
                .stream()
                .map(enrollment -> enrollment.getCourse().getId().toString())
                .collect(Collectors.toSet());

        if (!enrolledCourseIds.contains(exam.getCourseId())) {
            throw new RuntimeException("Student is not assigned to this exam");
        }
    }

    private boolean isExpired(ExamSession session) {
        return session.getExpiresAt() != null && LocalDateTime.now().isAfter(session.getExpiresAt());
    }

    private boolean isExpiredAndOpen(ExamSession session) {
        return session.getSubmittedAt() == null && isExpired(session);
    }

    private ExamSession autoSubmit(ExamSession session) {
        if (session.getSubmittedAt() == null) {
            session.setSubmittedAt(session.getExpiresAt() != null ? session.getExpiresAt() : LocalDateTime.now());
        }

        ExamSession savedSession = sessionRepository.save(session);
        try {
            sandboxService.teardownSandbox(
                    UUID.fromString(savedSession.getExamId()),
                    UUID.fromString(savedSession.getStudentId()));
        } catch (RuntimeException ignored) {
            // Session should still be treated as submitted even if the sandbox was already cleaned up.
        }
        return savedSession;
    }

    private void assertCurrentUserCanViewExamSessions() {
        if (currentUserService.hasRole(UserTypes.STUDENT)) {
            throw new RuntimeException("Students cannot view the full exam session list");
        }
    }

    private void assertCurrentUserCanAccessSession(ExamSession session) {
        if (currentUserService.hasRole(UserTypes.STUDENT)
                && !session.getStudentId().equals(currentUserService.requireCurrentUserId().toString())) {
            throw new RuntimeException("Students can only access their own sessions");
        }
    }

    private void assertCurrentUserCanAccessStudentId(String studentUserId) {
        UUID currentUserId = currentUserService.requireCurrentUserId();
        if (currentUserService.hasRole(UserTypes.STUDENT) && !currentUserId.toString().equals(studentUserId)) {
            throw new RuntimeException("Students can only access their own sessions");
        }
    }
}
