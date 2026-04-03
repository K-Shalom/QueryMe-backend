package com.year2.queryme.sandbox.controller;

import com.year2.queryme.sandbox.dto.ApiResponse;
import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
import com.year2.queryme.sandbox.dto.SandboxProvisionRequest;
import com.year2.queryme.sandbox.service.SandboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/sandboxes")
@RequiredArgsConstructor
public class SandboxController {

    private final SandboxService sandboxService;

    @PostMapping("/provision")
    public ResponseEntity<SandboxConnectionInfo> provisionSandbox(@RequestBody SandboxProvisionRequest request) {
        log.info("Provision request received for examId={} studentId={}", request.examId(), request.studentId());

        String schemaName = sandboxService.provisionSandbox(request.examId(), request.studentId(), request.seedSql());
        SandboxConnectionInfo connectionInfo = new SandboxConnectionInfo(schemaName, "level6year2");

        return ResponseEntity.status(HttpStatus.CREATED).body(connectionInfo);
    }

    @GetMapping("/{examId}/students/{studentId}")
    public ResponseEntity<SandboxConnectionInfo> getSandboxDetails(
            @PathVariable UUID examId,
            @PathVariable UUID studentId
    ) {
        log.info("Sandbox details requested for examId={} studentId={}", examId, studentId);
        SandboxConnectionInfo connectionInfo = sandboxService.getSandboxConnectionDetails(examId, studentId);
        return ResponseEntity.ok(connectionInfo);
    }

    @DeleteMapping("/{examId}/students/{studentId}")
    public ResponseEntity<ApiResponse> teardownSandbox(
            @PathVariable UUID examId,
            @PathVariable UUID studentId
    ) {
        log.info("Sandbox teardown requested for examId={} studentId={}", examId, studentId);
        sandboxService.teardownSandbox(examId, studentId);
        return ResponseEntity.ok(new ApiResponse("Sandbox successfully dropped for examId=%s and studentId=%s"
                .formatted(examId, studentId)));
    }
}

