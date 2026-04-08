package com.year2.queryme.repository;

import com.year2.queryme.model.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, String> {
    List<CourseEnrollment> findByCourseId(Long courseId);
    List<CourseEnrollment> findByStudentId(Long studentId);
    Optional<CourseEnrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);
}
