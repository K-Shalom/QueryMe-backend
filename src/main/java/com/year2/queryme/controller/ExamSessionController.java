package com.year2.queryme.controller;

import com.year2.queryme.model.dto.*;
import com.year2.queryme.service.ExamSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class ExamSessionController {

    private final ExamSessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<ExamSessionResponse> start(
            @RequestBody StartSessionRequest request) {
        return ResponseEntity.ok(sessionService.startSession(request));
    }

    @PatchMapping("/{sessionId}/submit")
    public ResponseEntity<ExamSessionResponse> submit(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.submitSession(sessionId));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ExamSessionResponse> getById(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.getSessionById(sessionId));
    }

    @GetMapping("/exam/{examId}")
    public ResponseEntity<Page<ExamSessionResponse>> getByExam(
            @PathVariable String examId,
            Pageable pageable) {
        return ResponseEntity.ok(sessionService.getSessionsByExam(examId, pageable));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<Page<ExamSessionResponse>> getByStudent(
            @PathVariable String studentId,
            Pageable pageable) {
        return ResponseEntity.ok(sessionService.getSessionsByStudent(studentId, pageable));
    }
}