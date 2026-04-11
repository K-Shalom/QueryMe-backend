package com.year2.queryme.service;

import com.year2.queryme.model.ExamSession;
import com.year2.queryme.repository.ExamSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ExamSessionExpirySchedulerTest {

    @Autowired
    private ExamSessionExpiryScheduler scheduler;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Test
    void autoSubmitExpiredSessionsContinuesWhenSandboxTeardownIsMissing() {
        examSessionRepository.deleteAll();

        ExamSession session = ExamSession.builder()
                .examId("44444444-4444-4444-4444-444444444444")
                .studentId("55555555-5555-5555-5555-555555555555")
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        ExamSession savedSession = examSessionRepository.save(session);

        assertDoesNotThrow(() -> scheduler.autoSubmitExpiredSessions());

        ExamSession reloadedSession = examSessionRepository.findById(savedSession.getId()).orElseThrow();
        assertNotNull(reloadedSession.getSubmittedAt());
    }
}