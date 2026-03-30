package com.year2.queryme.sandbox.service;

import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
import java.util.UUID;

public interface SandboxService {
    String provisionSandbox(UUID examId, UUID studentId, String seedSql);
    void teardownSandbox(UUID examId, UUID studentId);
    SandboxConnectionInfo getSandboxConnectionDetails(UUID examId, UUID studentId);
}