package com.year2.queryme.sandbox.service;

import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.repository.UserRepository;
import com.year2.queryme.sandbox.model.SandboxRegistry;
import com.year2.queryme.sandbox.repository.SandboxRegistryRepo;
import com.year2.queryme.sandbox.exception.SandboxExpiredException;
import com.year2.queryme.sandbox.exception.SandboxNotFoundException;
import com.year2.queryme.sandbox.exception.SandboxProvisioningException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class SandboxServiceImpl implements SandboxService {

    private final SandboxRegistryRepo registryRepo;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final String dbUsername;

    // Uses the standard Spring Boot database connection automatically
    public SandboxServiceImpl(
            SandboxRegistryRepo registryRepo,
            ExamRepository examRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${spring.datasource.username}") String dbUsername
    ) {
        this.registryRepo = registryRepo;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.dbUsername = dbUsername;
    }

    @Override
    @Transactional
    public String provisionSandbox(UUID examId, UUID studentId, String seedSql) {
        SandboxRegistry existingRegistry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .filter(registry -> "ACTIVE".equals(registry.getStatus()))
                .orElse(null);

        if (existingRegistry != null) {
            log.info("Reusing existing active sandbox schema: {}", existingRegistry.getSchemaName());
            return existingRegistry.getSchemaName();
        }

        var exam = examRepository.findById(examId.toString())
                .orElseThrow(() -> new SandboxProvisioningException("Exam not found in registry"));

        userRepository.findById(studentId)
                .orElseThrow(() -> new SandboxProvisioningException("Student not found in Auth registry"));

        String studentNumber = studentRepository.findByUser_Id(studentId)
                .map(student -> student.getStudentNumber())
                .orElseThrow(() -> new SandboxProvisioningException("Student profile is missing"));

        String safeStudentNumber = studentNumber.replaceAll("[^a-zA-Z0-9]", "");

        Long sequenceNumber = jdbcTemplate.queryForObject("SELECT nextval('sandbox_schema_seq')", Long.class);
        if (sequenceNumber == null) {
            throw new SandboxProvisioningException("Failed to generate sandbox schema sequence number");
        }

        String schemaName = "s_" + safeStudentNumber + "_" + sequenceNumber;

        log.info("Provisioning sandbox schema: {}", schemaName);

        try {
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

            if (seedSql != null && !seedSql.trim().isEmpty()) {
                jdbcTemplate.execute("SET search_path TO " + schemaName);
                try {
                    jdbcTemplate.execute(seedSql);
                } finally {
                    jdbcTemplate.execute("SET search_path TO public");
                }
            }

            SandboxRegistry registry = new SandboxRegistry();
            registry.setExamId(examId);
            registry.setStudentId(studentId);
            registry.setSchemaName(schemaName);
            registry.setDbUser(dbUsername);
            registry.setStatus("ACTIVE");
            registry.setExpiresAt(exam.getTimeLimitMins() != null
                    ? LocalDateTime.now().plusMinutes(exam.getTimeLimitMins())
                    : null);
            registryRepo.save(registry);

            return schemaName;

        } catch (Exception e) {
            log.error("Failed to provision sandbox: {}", schemaName, e);
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
            throw new SandboxProvisioningException("Sandbox provisioning failed", e);
        }
    }

    @Override
    @Transactional
    public void teardownSandbox(UUID examId, UUID studentId) {
        SandboxRegistry registry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new SandboxNotFoundException("Sandbox not found for given Exam and Student"));

        log.info("Tearing down sandbox schema: {}", registry.getSchemaName());

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + registry.getSchemaName() + " CASCADE");

        registry.setStatus("DROPPED");
        registryRepo.save(registry);
    }

    @Override
    public SandboxConnectionInfo getSandboxConnectionDetails(UUID examId, UUID studentId) {
        SandboxRegistry registry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new SandboxNotFoundException("Sandbox not found"));

        if (!"ACTIVE".equals(registry.getStatus())) {
            throw new SandboxExpiredException("Sandbox is not active.");
        }

        return new SandboxConnectionInfo(registry.getSchemaName(), registry.getDbUser());
    }
}
