package com.year2.queryme.controller;

import com.year2.queryme.model.dto.*;
import com.year2.queryme.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
    public ResponseEntity<ExamResponse> create(@RequestBody CreateExamRequest request) {
        return ResponseEntity.ok(examService.createExam(request));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ExamResponse> getById(@PathVariable String examId) {
        return ResponseEntity.ok(examService.getExamById(examId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ExamResponse>> getByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(examService.getExamsByCourse(courseId));
    }

    @GetMapping("/published")
    public ResponseEntity<List<ExamResponse>> getPublished() {
        return ResponseEntity.ok(examService.getPublishedExams());
    }

    @PutMapping("/{examId}")
    public ResponseEntity<ExamResponse> update(
            @PathVariable String examId,
            @RequestBody UpdateExamRequest request) {
        return ResponseEntity.ok(examService.updateExam(examId, request));
    }

    @PatchMapping("/{examId}/publish")
    public ResponseEntity<ExamResponse> publish(@PathVariable String examId) {
        return ResponseEntity.ok(examService.publishExam(examId));
    }

    @PatchMapping("/{examId}/unpublish")
    public ResponseEntity<ExamResponse> unpublish(@PathVariable String examId) {
        return ResponseEntity.ok(examService.unpublishExam(examId));
    }

    @PatchMapping("/{examId}/close")
    public ResponseEntity<ExamResponse> close(@PathVariable String examId) {
        return ResponseEntity.ok(examService.closeExam(examId));
    }

    @DeleteMapping("/{examId}")
    public ResponseEntity<Void> delete(@PathVariable String examId) {
        examService.deleteExam(examId);
        return ResponseEntity.noContent().build();
    }
}