package com.year2.queryme.sandbox.service;

import com.year2.queryme.sandbox.model.SandboxRegistry;
import com.year2.queryme.sandbox.repository.SandboxRegistryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SandboxCleanupSchedulerTest {

    @Mock
    private SandboxRegistryRepo registryRepo;

    @Mock
    private SandboxService sandboxService;

    @InjectMocks
    private SandboxCleanupScheduler cleanupScheduler;

    private List<SandboxRegistry> expiredSandboxes;
    private SandboxRegistry activeSandbox;
    private SandboxRegistry expiredSandbox1;
    private SandboxRegistry expiredSandbox2;

    @BeforeEach
    void setUp() {
        // Create active sandbox (should not be cleaned up)
        activeSandbox = new SandboxRegistry();
        activeSandbox.setId(UUID.randomUUID());
        activeSandbox.setExamId(UUID.randomUUID());
        activeSandbox.setStudentId(UUID.randomUUID());
        activeSandbox.setSchemaName("exam_active_student_active");
        activeSandbox.setDbUser("usr_exam_active_student_active");
        activeSandbox.setStatus("ACTIVE");
        activeSandbox.setExpiresAt(LocalDateTime.now().plusHours(1)); // Not expired

        // Create expired sandboxes (should be cleaned up)
        expiredSandbox1 = new SandboxRegistry();
        expiredSandbox1.setId(UUID.randomUUID());
        expiredSandbox1.setExamId(UUID.randomUUID());
        expiredSandbox1.setStudentId(UUID.randomUUID());
        expiredSandbox1.setSchemaName("exam_expired1_student_expired1");
        expiredSandbox1.setDbUser("usr_exam_expired1_student_expired1");
        expiredSandbox1.setStatus("ACTIVE");
        expiredSandbox1.setExpiresAt(LocalDateTime.now().minusMinutes(30)); // Expired 30 minutes ago

        expiredSandbox2 = new SandboxRegistry();
        expiredSandbox2.setId(UUID.randomUUID());
        expiredSandbox2.setExamId(UUID.randomUUID());
        expiredSandbox2.setStudentId(UUID.randomUUID());
        expiredSandbox2.setSchemaName("exam_expired2_student_expired2");
        expiredSandbox2.setDbUser("usr_exam_expired2_student_expired2");
        expiredSandbox2.setStatus("ACTIVE");
        expiredSandbox2.setExpiresAt(LocalDateTime.now().minusHours(2)); // Expired 2 hours ago

        expiredSandboxes = Arrays.asList(expiredSandbox1, expiredSandbox2);
    }

    @Test
    void testCleanupExpiredSandboxes_Success() {
        // Given
        when(registryRepo.findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class)))
                .thenReturn(expiredSandboxes);

        // When
        cleanupScheduler.cleanupExpiredSandboxes();

        // Then
        verify(registryRepo).findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class));
        
        // Verify teardown is called for each expired sandbox
        verify(sandboxService).teardownSandbox(expiredSandbox1.getExamId(), expiredSandbox1.getStudentId());
        verify(sandboxService).teardownSandbox(expiredSandbox2.getExamId(), expiredSandbox2.getStudentId());
        verify(sandboxService, times(2)).teardownSandbox(any(UUID.class), any(UUID.class));
    }

    @Test
    void testCleanupExpiredSandboxes_NoExpiredSandboxes() {
        // Given
        when(registryRepo.findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class)))
                .thenReturn(Arrays.asList()); // Empty list

        // When
        cleanupScheduler.cleanupExpiredSandboxes();

        // Then
        verify(registryRepo).findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class));
        
        // Verify no teardown operations
        verify(sandboxService, never()).teardownSandbox(any(UUID.class), any(UUID.class));
    }

    @Test
    void testCleanupExpiredSandboxes_WithMixedSandboxes() {
        // Given - Return both expired and active sandboxes, but only expired ones should be returned by the query
        when(registryRepo.findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredSandbox1)); // Only expired ones

        // When
        cleanupScheduler.cleanupExpiredSandboxes();

        // Then
        verify(registryRepo).findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class));
        
        // Verify only expired sandbox is cleaned up
        verify(sandboxService).teardownSandbox(expiredSandbox1.getExamId(), expiredSandbox1.getStudentId());
        verify(sandboxService, never()).teardownSandbox(activeSandbox.getExamId(), activeSandbox.getStudentId());
        verify(sandboxService, times(1)).teardownSandbox(any(UUID.class), any(UUID.class));
    }

    @Test
    void testCleanupExpiredSandboxes_TeardownFails_ContinuesWithOthers() {
        // Given
        when(registryRepo.findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class)))
                .thenReturn(expiredSandboxes);
        
        // Make first teardown fail
        doThrow(new RuntimeException("Teardown failed"))
                .when(sandboxService).teardownSandbox(expiredSandbox1.getExamId(), expiredSandbox1.getStudentId());

        // When
        cleanupScheduler.cleanupExpiredSandboxes();

        // Then
        verify(registryRepo).findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class));
        
        // Verify both teardown attempts are made (one fails, one succeeds)
        verify(sandboxService).teardownSandbox(expiredSandbox1.getExamId(), expiredSandbox1.getStudentId());
        verify(sandboxService).teardownSandbox(expiredSandbox2.getExamId(), expiredSandbox2.getStudentId());
        verify(sandboxService, times(2)).teardownSandbox(any(UUID.class), any(UUID.class));
    }

    @Test
    void testCleanupExpiredSandboxes_NullExpiredList() {
        // Given
        when(registryRepo.findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class)))
                .thenReturn(null);

        // When
        cleanupScheduler.cleanupExpiredSandboxes();

        // Then
        verify(registryRepo).findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class));
        
        // Should not throw exception and should not attempt any teardowns
        verify(sandboxService, never()).teardownSandbox(any(UUID.class), any(UUID.class));
    }

    @Test
    void testCleanupExpiredSandboxes_VerifyCorrectTimeParameter() {
        // Given
        when(registryRepo.findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // When
        cleanupScheduler.cleanupExpiredSandboxes();

        // Then
        verify(registryRepo).findByStatusAndExpiresAtBefore(eq("ACTIVE"), any(LocalDateTime.class));
        
        // Verify the time parameter is "now" (current time)
        // We can't easily test the exact time, but we can verify it's called with some LocalDateTime
        verify(registryRepo, times(1)).findByStatusAndExpiresAtBefore(any(String.class), any(LocalDateTime.class));
    }

    @Test
    void testCleanupExpiredSandboxes_LargeNumberOfSandboxes() {
        // Given - Create many expired sandboxes
        SandboxRegistry[] manyExpiredSandboxes = new SandboxRegistry[50];
        for (int i = 0; i < 50; i++) {
            SandboxRegistry sandbox = new SandboxRegistry();
            sandbox.setId(UUID.randomUUID());
            sandbox.setExamId(UUID.randomUUID());
            sandbox.setStudentId(UUID.randomUUID());
            sandbox.setSchemaName("exam_expired" + i + "_student_expired" + i);
            sandbox.setDbUser("usr_exam_expired" + i + "_student_expired" + i);
            sandbox.setStatus("ACTIVE");
            sandbox.setExpiresAt(LocalDateTime.now().minusMinutes(i)); // All expired
            manyExpiredSandboxes[i] = sandbox;
        }

        when(registryRepo.findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(manyExpiredSandboxes));

        // When
        cleanupScheduler.cleanupExpiredSandboxes();

        // Then
        verify(registryRepo).findByStatusAndExpiresAtBefore("ACTIVE", any(LocalDateTime.class));
        
        // Verify teardown is called for all 50 sandboxes
        verify(sandboxService, times(50)).teardownSandbox(any(UUID.class), any(UUID.class));
    }
}
