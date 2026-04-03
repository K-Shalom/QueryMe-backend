package com.year2.queryme.sandbox.dto;

import java.util.UUID;

public record SandboxProvisionRequest(
        UUID examId,
        UUID studentId,
        String seedSql
) {
}

