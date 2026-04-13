package com.year2.queryme.repository;

import com.year2.queryme.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
	Optional<Student> findByUser_Id(UUID userId);
    boolean existsByStudentNumber(String studentNumber);
}
