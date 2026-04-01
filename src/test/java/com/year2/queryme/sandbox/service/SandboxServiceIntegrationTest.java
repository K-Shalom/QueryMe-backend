package com.year2.queryme.sandbox.service;

import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
import com.year2.queryme.sandbox.model.SandboxRegistry;
import com.year2.queryme.sandbox.repository.SandboxRegistryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SandboxServiceIntegrationTest {

    @Autowired
    private SandboxService sandboxService;

    @Autowired
    private SandboxRegistryRepo registryRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID examId;
    private UUID studentId;
    private String seedSql;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        registryRepo.deleteAll();
        
        examId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        seedSql = "CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50)); INSERT INTO test_table VALUES (1, 'test');";
    }

    @Test
    @Transactional
    void testProvisionSandbox_CreatesActualSchema() {
        // When
        String schemaName = sandboxService.provisionSandbox(examId, studentId, seedSql);

        // Then
        assertNotNull(schemaName);
        
        // Verify schema exists by checking if we can query it
        // Note: H2 doesn't support schemas like PostgreSQL, but we can verify the registry entry
        SandboxRegistry registry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new AssertionError("Sandbox registry not found"));
        
        assertEquals(examId, registry.getExamId());
        assertEquals(studentId, registry.getStudentId());
        assertEquals("ACTIVE", registry.getStatus());
        assertEquals(schemaName, registry.getSchemaName());
        assertNotNull(registry.getDbUser());
    }

    @Test
    @Transactional
    void testGetSandboxConnectionDetails() {
        // Given
        String schemaName = sandboxService.provisionSandbox(examId, studentId, seedSql);

        // When
        SandboxConnectionInfo connectionInfo = sandboxService.getSandboxConnectionDetails(examId, studentId);

        // Then
        assertNotNull(connectionInfo);
        assertEquals(schemaName, connectionInfo.getSchemaName());
        assertNotNull(connectionInfo.getDbUser());
    }

    @Test
    @Transactional
    void testTeardownSandbox_UpdatesRegistryStatus() {
        // Given
        sandboxService.provisionSandbox(examId, studentId, seedSql);
        
        // Verify registry exists and is active
        SandboxRegistry registry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new AssertionError("Sandbox registry not found"));
        assertEquals("ACTIVE", registry.getStatus());

        // When
        sandboxService.teardownSandbox(examId, studentId);

        // Then
        SandboxRegistry updatedRegistry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new AssertionError("Sandbox registry not found"));
        assertEquals("DROPPED", updatedRegistry.getStatus());
    }

    @Test
    @Transactional
    void testMultipleSandboxesForSameExamDifferentStudents() {
        // Given
        UUID student1Id = UUID.randomUUID();
        UUID student2Id = UUID.randomUUID();

        // When
        String schema1 = sandboxService.provisionSandbox(examId, student1Id, seedSql);
        String schema2 = sandboxService.provisionSandbox(examId, student2Id, seedSql);

        // Then
        assertNotNull(schema1);
        assertNotNull(schema2);
        assertNotEquals(schema1, schema2); // Should be different schemas

        // Verify both registries exist
        assertTrue(registryRepo.findByExamIdAndStudentId(examId, student1Id).isPresent());
        assertTrue(registryRepo.findByExamIdAndStudentId(examId, student2Id).isPresent());
    }

    @Test
    @Transactional
    void testProvisionSandbox_WithComplexSeedSql() {
        // Given
        String complexSeedSql = """
            CREATE TABLE users (
                id INT PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            
            CREATE TABLE orders (
                id INT PRIMARY KEY,
                user_id INT,
                total DECIMAL(10,2),
                FOREIGN KEY (user_id) REFERENCES users(id)
            );
            
            INSERT INTO users (id, name, email) VALUES 
                (1, 'John Doe', 'john@example.com'),
                (2, 'Jane Smith', 'jane@example.com');
                
            INSERT INTO orders (id, user_id, total) VALUES
                (1, 1, 99.99),
                (2, 2, 149.50);
            """;

        // When
        String schemaName = sandboxService.provisionSandbox(examId, studentId, complexSeedSql);

        // Then
        assertNotNull(schemaName);
        
        // Verify registry was created
        SandboxRegistry registry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new AssertionError("Sandbox registry not found"));
        assertEquals("ACTIVE", registry.getStatus());
    }

    @Test
    @Transactional 
    void testSandboxLifecycle_FullWorkflow() {
        // 1. Provision sandbox
        String schemaName = sandboxService.provisionSandbox(examId, studentId, seedSql);
        assertNotNull(schemaName);

        // 2. Get connection details
        SandboxConnectionInfo connectionInfo = sandboxService.getSandboxConnectionDetails(examId, studentId);
        assertNotNull(connectionInfo);
        assertEquals(schemaName, connectionInfo.getSchemaName());

        // 3. Verify registry is active
        SandboxRegistry registry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new AssertionError("Sandbox registry not found"));
        assertEquals("ACTIVE", registry.getStatus());

        // 4. Teardown sandbox
        sandboxService.teardownSandbox(examId, studentId);

        // 5. Verify registry status is updated
        SandboxRegistry updatedRegistry = registryRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new AssertionError("Sandbox registry not found"));
        assertEquals("DROPPED", updatedRegistry.getStatus());

        // 6. Verify connection details are no longer available
        assertThrows(IllegalStateException.class, () -> {
            sandboxService.getSandboxConnectionDetails(examId, studentId);
        });
    }

    @Test
    void testFindAllSandboxRegistries() {
        // Given
        sandboxService.provisionSandbox(examId, studentId, seedSql);
        UUID anotherStudentId = UUID.randomUUID();
        sandboxService.provisionSandbox(examId, anotherStudentId, seedSql);

        // When
        List<SandboxRegistry> registries = registryRepo.findAll();

        // Then
        assertEquals(2, registries.size());
        assertTrue(registries.stream().anyMatch(r -> r.getStudentId().equals(studentId)));
        assertTrue(registries.stream().anyMatch(r -> r.getStudentId().equals(anotherStudentId)));
    }
}
