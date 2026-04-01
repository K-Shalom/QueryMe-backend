package com.year2.queryme.repository;

import com.year2.queryme.model.Exam;
import com.year2.queryme.model.enums.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, String> {
    List<Exam> findByCourseId(String courseId);
    List<Exam> findByStatus(ExamStatus status);
    List<Exam> findByCourseIdAndStatus(String courseId, ExamStatus status);
}