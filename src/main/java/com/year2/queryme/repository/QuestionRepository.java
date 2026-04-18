package com.year2.queryme.repository;

import com.year2.queryme.model.Question;
import com.year2.queryme.repository.projection.QuestionSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByExamIdOrderByOrderIndexAsc(UUID examId);
    Page<Question> findByExamIdOrderByOrderIndexAsc(UUID examId, Pageable pageable);
    @Query("""
            select q.id as id,
                   q.prompt as prompt,
                   q.marks as marks
            from Question q
            where q.examId = :examId
            """)
    List<QuestionSummaryView> findQuestionSummariesByExamId(@Param("examId") UUID examId);
    long countByExamId(UUID examId);

    @Query("select q.examId, count(q) from Question q where q.examId in :examIds group by q.examId")
    List<Object[]> countByExamIds(@Param("examIds") Collection<UUID> examIds);
}
