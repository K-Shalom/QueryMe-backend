package com.year2.queryme.repository;

import com.year2.queryme.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResultRepository extends JpaRepository<Result, UUID> {
    List<Result> findBySessionId(UUID sessionId);
    List<Result> findByQuestionId(UUID questionId);
    List<Result> findAllByExamId(UUID examId);
    Optional<Result> findBySubmissionId(UUID submissionId);
}
