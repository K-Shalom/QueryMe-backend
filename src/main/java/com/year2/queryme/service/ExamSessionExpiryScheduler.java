package com.year2.queryme.service;

import com.year2.queryme.model.ExamSession;
import com.year2.queryme.repository.ExamSessionRepository;
import com.year2.queryme.sandbox.service.SandboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamSessionExpiryScheduler {

    private final ExamSessionRepository examSessionRepository;
    private final SandboxService sandboxService;

    @Scheduled(fixedRate = 60000)
    public void autoSubmitExpiredSessions() {
        List<ExamSession> expiredSessions = examSessionRepository
                .findBySubmittedAtIsNullAndExpiresAtBefore(Instant.now());

        for (ExamSession session : expiredSessions) {
            try {
                session.setSubmittedAt(session.getExpiresAt() != null ? session.getExpiresAt() : Instant.now());
                examSessionRepository.save(session);
                sandboxService.teardownSandbox(
                        UUID.fromString(session.getExamId()),
                        UUID.fromString(session.getStudentId()));
                log.info("Auto-submitted expired session {}", session.getId());
            } catch (Exception ex) {
                log.warn("Failed to auto-submit expired session {}: {}", session.getId(), ex.getMessage());
            }
        }
    }
}
