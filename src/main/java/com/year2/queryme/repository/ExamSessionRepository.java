package com.year2.queryme.repository;

import com.year2.queryme.model.ExamSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExamSessionRepository extends JpaRepository<ExamSession, String> {
    Optional<ExamSession> findByExamIdAndStudentId(String examId, String studentId);
    List<ExamSession> findByExamId(String examId);
    List<ExamSession> findByStudentId(String studentId);
    Page<ExamSession> findByExamId(String examId, Pageable pageable);
    Page<ExamSession> findByStudentId(String studentId, Pageable pageable);
    boolean existsByExamIdAndStudentId(String examId, String studentId);
    List<ExamSession> findByExamIdAndStudentIdOrderByStartedAtDesc(String examId, String studentId);
    Optional<ExamSession> findFirstByExamIdAndStudentIdAndSubmittedAtIsNullOrderByStartedAtDesc(String examId, String studentId);
    List<ExamSession> findBySubmittedAtIsNullAndExpiresAtBefore(Instant time);
}
