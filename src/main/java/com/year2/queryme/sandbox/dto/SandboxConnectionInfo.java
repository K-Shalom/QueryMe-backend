package com.year2.queryme.sandbox.dto;

public record SandboxConnectionInfo(
        String schemaName,
        String dbUsername
) {
    public String getSchemaName() {
        return null;
    }

    public String getDbUser() {
        return "";
    }
}