package com.year2.queryme.sandbox.service;

import com.year2.queryme.sandbox.model.SandboxRegistry;
import com.year2.queryme.sandbox.repository.SandboxRegistryRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class SandboxCleanupScheduler {

    private final SandboxRegistryRepo registryRepo;
    private final SandboxService sandboxService;

    public SandboxCleanupScheduler(SandboxRegistryRepo registryRepo, SandboxService sandboxService) {
        this.registryRepo = registryRepo;
        this.sandboxService = sandboxService;
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredSandboxes() {
        log.info("Running background cleanup for expired sandboxes...");

        List<SandboxRegistry> expiredSandboxes = registryRepo
                .findByStatusAndExpiresAtBefore("ACTIVE", Instant.now());

        for (SandboxRegistry sandbox : expiredSandboxes) {
            try {
                sandboxService.teardownSandbox(sandbox.getExamId(), sandbox.getStudentId());
                log.info("Successfully cleaned up expired sandbox: {}", sandbox.getSchemaName());
            } catch (Exception e) {
                log.error("Failed to clean up sandbox: {}", sandbox.getSchemaName(), e);
            }
        }
    }
}