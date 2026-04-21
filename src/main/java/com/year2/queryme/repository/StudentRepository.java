package com.year2.queryme.repository;

import com.year2.queryme.model.Student;
import com.year2.queryme.repository.projection.StudentNameView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
	Optional<Student> findByUser_Id(UUID userId);
	List<Student> findByUser_IdIn(Collection<UUID> userIds);
    Page<Student> findAll(Pageable pageable);
	@Query("""
			select s.user.id as userId,
				   s.fullName as fullName
			from Student s
			where s.user.id in :userIds
			""")
	List<StudentNameView> findStudentNamesByUserIds(@Param("userIds") Collection<UUID> userIds);
    boolean existsByStudentNumber(String studentNumber);

    /** Returns all students whose linked user account has the STUDENT role. */
    @Query("SELECT s FROM Student s WHERE s.user.role = com.year2.queryme.model.enums.UserTypes.STUDENT")
    Page<Student> findAllStudents(Pageable pageable);

    @Query("""
            SELECT DISTINCT s FROM Student s
            JOIN CourseEnrollment ce ON s.id = ce.student.id
            WHERE ce.course.teacher.user.email = :teacherEmail
            """)
    Page<Student> findAllByTeacherEmail(@Param("teacherEmail") String teacherEmail, Pageable pageable);
}
