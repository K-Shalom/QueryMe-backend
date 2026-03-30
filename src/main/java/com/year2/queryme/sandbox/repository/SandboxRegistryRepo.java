package com.year2.queryme.sandbox.repository;

import com.year2.queryme.sandbox.model.SandboxRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SandboxRegistryRepo extends JpaRepository<SandboxRegistry, UUID> {
    Optional<SandboxRegistry> findByExamIdAndStudentId(UUID examId, UUID studentId);
    List<SandboxRegistry> findByStatusAndExpiresAtBefore(String status, LocalDateTime time);
}