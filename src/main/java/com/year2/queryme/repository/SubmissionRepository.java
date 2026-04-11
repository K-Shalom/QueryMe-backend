package com.year2.queryme.repository;

import com.year2.queryme.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByStudentIdAndExamId(UUID studentId, UUID examId);
    List<Submission> findByExamId(UUID examId);
    List<Submission> findByQuestionId(UUID questionId);
    List<Submission> findBySessionIdOrderBySubmittedAtDesc(UUID sessionId);
    List<Submission> findByExamIdOrderBySubmittedAtDesc(UUID examId);
}
