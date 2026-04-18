package com.year2.queryme.repository;

import com.year2.queryme.model.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, String> {
    List<CourseEnrollment> findByCourseId(Long courseId);
    List<CourseEnrollment> findByStudentId(Long studentId);
    Page<CourseEnrollment> findByCourseId(Long courseId, Pageable pageable);
    Page<CourseEnrollment> findByStudentId(Long studentId, Pageable pageable);
    Optional<CourseEnrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);
}
