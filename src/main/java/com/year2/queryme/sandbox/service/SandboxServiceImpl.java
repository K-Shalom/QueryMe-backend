package com.year2.queryme.sandbox.service;

import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
import com.year2.queryme.sandbox.model.SandboxRegistry;
import com.year2.queryme.sandbox.repository.SandboxRegistryRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class SandboxServiceImpl implements SandboxService {

    private final SandboxRegistryRepo registryRepo;
    private final JdbcTemplate jdbcTemplate;

    // Uses the standard Spring Boot database connection automatically
    public SandboxServiceImpl(SandboxRegistryRepo registryRepo, JdbcTemplate jdbcTemplate) {
        this.registryRepo = registryRepo;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public String provisionSandbox(UUID examId, UUID studentId, String seedSql) {
        String schemaName = "exam_" + examId.toString().replace("-", "") +
                "_student_" + studentId.toString().replace("-", "");
        String dbUser = "usr_" + schemaName.substring(0, Math.min(schemaName.length(), 50));
        String dbPassword = UUID.randomUUID().toString();

        log.info("Provisioning sandbox schema: {}", schemaName);

        try {
            jdbcTemplate.execute("CREATE SCHEMA " + schemaName);
            jdbcTemplate.execute("CREATE USER " + dbUser + " WITH PASSWORD '" + dbPassword + "'");

            // SECURITY: Explicitly block this user from seeing the main application tables
            jdbcTemplate.execute("REVOKE ALL ON SCHEMA public FROM " + dbUser);

            // Grant access ONLY to their specific sandbox schema
            jdbcTemplate.execute("GRANT USAGE ON SCHEMA " + schemaName + " TO " + dbUser);
            jdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA " + schemaName + " TO " + dbUser);
            jdbcTemplate.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA " + schemaName + " GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO " + dbUser);

            if (seedSql != null && !seedSql.trim().isEmpty()) {
                jdbcTemplate.execute("SET search_path TO " + schemaName);
                jdbcTemplate.execute(seedSql);
                jdbcTemplate.execute("SET search_path TO public");
            }

            SandboxRegistry registry = new SandboxRegistry();
            registry.setExamId(examId);
            registry.setStudentId(studentId);
            registry.setSchemaName(schemaName);
            registry.setDbUser(dbUser);
            registry.setStatus("ACTIVE");
            registryRepo.save(registry);

            return schemaName;

        } catch (Exception e) {
            log.error("Failed to provision sandbox: {}", schemaName, e);
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
            jdbcTemplate.execute("DROP USER IF EXISTS " + dbUser);
            throw new RuntimeException("Sandbox provisioning failed", e);
        }
    }

    @Override
    @Transactional
    public void teardownSandbox(UUID examId, UUID studentId) {
        SandboxRegistry registry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Sandbox not found for given Exam and Student"));

        log.info("Tearing down sandbox schema: {}", registry.getSchemaName());

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + registry.getSchemaName() + " CASCADE");
        jdbcTemplate.execute("DROP USER IF EXISTS " + registry.getDbUser());

        registry.setStatus("DROPPED");
        registryRepo.save(registry);
    }

    @Override
    public SandboxConnectionInfo getSandboxConnectionDetails(UUID examId, UUID studentId) {
        SandboxRegistry registry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Sandbox not found"));

        if (!"ACTIVE".equals(registry.getStatus())) {
            throw new IllegalStateException("Sandbox is not active.");
        }

        return new SandboxConnectionInfo(registry.getSchemaName(), registry.getDbUser());
    }
}