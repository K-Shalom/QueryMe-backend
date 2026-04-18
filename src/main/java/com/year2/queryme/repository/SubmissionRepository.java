package com.year2.queryme.repository;

import com.year2.queryme.model.Submission;
import com.year2.queryme.repository.projection.TeacherDashboardSubmissionView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            select s.studentId as studentId,
                   s.sessionId as sessionId,
                   s.questionId as questionId,
                   s.score as score,
                   s.isCorrect as isCorrect,
                   s.submittedQuery as submittedQuery,
                   s.submittedAt as submittedAt
            from Submission s
            where s.examId = :examId
            order by s.submittedAt desc
            """)
    List<TeacherDashboardSubmissionView> findDashboardRowsByExamIdOrderBySubmittedAtDesc(@Param("examId") UUID examId);
}
