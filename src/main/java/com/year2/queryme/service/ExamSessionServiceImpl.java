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
import com.year2.queryme.repository.ExamAttemptOverrideRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.model.ExamAttemptOverride;
import com.year2.queryme.sandbox.service.SandboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
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
    private final ExamAttemptOverrideRepository attemptOverrideRepository;
    private final com.year2.queryme.repository.CourseRepository courseRepository;
    private final com.year2.queryme.repository.ResultRepository resultRepository;

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

        int maxAttempts = exam.getMaxAttempts();
        int additionalAttempts = attemptOverrideRepository.findByExamIdAndStudentId(request.getExamId(), request.getStudentId())
                .map(ExamAttemptOverride::getAdditionalAttempts)
                .orElse(0);

        if (existingSessions.size() >= (maxAttempts + additionalAttempts)) {
            throw new RuntimeException("Maximum attempts reached for this exam");
        }

        Instant now = Instant.now();
        Instant expiresAt = exam.getTimeLimitMins() != null
                ? now.plus(java.time.Duration.ofMinutes(exam.getTimeLimitMins()))
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

        session.setSubmittedAt(Instant.now());
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
        if (currentUserService.hasRole(UserTypes.TEACHER)) {
            assertTeacherOwnsExam(examId);
        }
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
                .map(session -> {
                    ExamSessionResponse res = ExamSessionMapper.toResponse(session);
                    try {
                        List<com.year2.queryme.model.Result> results = resultRepository.findBySessionId(UUID.fromString(session.getId()));
                        if (!results.isEmpty()) {
                            res.setTotalScore(results.stream().mapToInt(com.year2.queryme.model.Result::getScore).sum());
                            res.setTotalMaxScore(results.stream().mapToInt(com.year2.queryme.model.Result::getMaxScore).sum());
                        }
                    } catch (Exception ignored) {}
                    return res;
                });
    }

    @Override
    @Transactional
    public ExamSessionResponse extendSession(String sessionId, int extraHours) {
        ExamSession session = findById(sessionId);
        if (currentUserService.hasRole(UserTypes.TEACHER)) {
            assertTeacherOwnsExam(session.getExamId());
        }
        
        if (currentUserService.hasRole(UserTypes.STUDENT)) {
            throw new RuntimeException("Students cannot extend sessions");
        }

        Instant newExpiresAt;
        if (session.getExpiresAt() != null) {
            if (session.getExpiresAt().isBefore(Instant.now())) {
                newExpiresAt = Instant.now().plus(java.time.Duration.ofHours(extraHours));
            } else {
                newExpiresAt = session.getExpiresAt().plus(java.time.Duration.ofHours(extraHours));
            }
            session.setExpiresAt(newExpiresAt);
        } else {
            session.setExpiresAt(Instant.now().plus(java.time.Duration.ofHours(extraHours)));
        }

        // Re-open session if it was submitted
        if (session.getSubmittedAt() != null) {
            session.setSubmittedAt(null);
            
            // Re-provision sandbox if it was torn down
            try {
                Exam exam = examRepository.findById(session.getExamId())
                        .orElseThrow(() -> new RuntimeException("Exam not found"));
                sandboxService.provisionSandbox(
                        UUID.fromString(session.getExamId()),
                        UUID.fromString(session.getStudentId()),
                        exam.getSeedSql());
            } catch (Exception e) {
                // Ignore if sandbox still exists
            }
        }

        return ExamSessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional
    @CacheEvict(value = "student-results", key = "#sessionId")
    public ExamSessionResponse addFeedback(String sessionId, String feedback) {
        ExamSession session = findById(sessionId);
        if (currentUserService.hasRole(UserTypes.TEACHER)) {
            assertTeacherOwnsExam(session.getExamId());
        }
        
        if (currentUserService.hasRole(UserTypes.STUDENT)) {
            throw new RuntimeException("Students cannot add feedback to sessions");
        }

        session.setTeacherFeedback(feedback);
        return ExamSessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional
    public void grantAdditionalAttempts(String examId, String studentId, int additionalAttempts) {
        if (currentUserService.hasRole(UserTypes.TEACHER)) {
            assertTeacherOwnsExam(examId);
        }
        if (currentUserService.hasRole(UserTypes.STUDENT)) {
            throw new RuntimeException("Students cannot grant additional attempts");
        }

        ExamAttemptOverride override = attemptOverrideRepository.findByExamIdAndStudentId(examId, studentId)
                .orElse(ExamAttemptOverride.builder()
                        .examId(examId)
                        .studentId(studentId)
                        .additionalAttempts(0)
                        .build());

        override.setAdditionalAttempts(override.getAdditionalAttempts() + additionalAttempts);
        attemptOverrideRepository.save(override);
    }

    @Override
    @Transactional(readOnly = true)
    public int getAdditionalAttempts(String examId, String studentId) {
        if (currentUserService.hasRole(UserTypes.TEACHER)) {
            assertTeacherOwnsExam(examId);
        }
        
        return attemptOverrideRepository.findByExamIdAndStudentId(examId, studentId)
                .map(ExamAttemptOverride::getAdditionalAttempts)
                .orElse(0);
    }

    @Override
    @Transactional
    public void heartbeat(String sessionId) {
        ExamSession session = findById(sessionId);
        assertCurrentUserCanAccessSession(session);
        session.setLastHeartbeatAt(Instant.now());
        sessionRepository.save(session);
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
        if (session.getExpiresAt() == null) return false;
        // 10-second grace period to account for network latency
        return Instant.now().isAfter(session.getExpiresAt().plus(java.time.Duration.ofSeconds(10)));
    }

    private boolean isExpiredAndOpen(ExamSession session) {
        return session.getSubmittedAt() == null && isExpired(session);
    }

    private ExamSession autoSubmit(ExamSession session) {
        if (session.getSubmittedAt() == null) {
            session.setSubmittedAt(session.getExpiresAt() != null ? session.getExpiresAt() : Instant.now());
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

    private void assertTeacherOwnsExam(String examId) {
        if (currentUserService.hasRole(UserTypes.TEACHER)) {
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new RuntimeException("Exam not found: " + examId));
            
            com.year2.queryme.model.Course course = courseRepository.findById(Long.parseLong(exam.getCourseId()))
                    .orElseThrow(() -> new RuntimeException("Course not found: " + exam.getCourseId()));
            
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            if (!course.getTeacher().getUser().getEmail().equals(email)) {
                throw new RuntimeException("Teachers can only manage sessions for their own exams");
            }
        }
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
