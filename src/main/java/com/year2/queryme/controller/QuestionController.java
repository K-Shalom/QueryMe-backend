package com.year2.queryme.controller;

import com.year2.queryme.model.dto.QuestionRequest;
import com.year2.queryme.model.dto.QuestionResponse;
import com.year2.queryme.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exams/{examId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER', 'TEACHER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<QuestionResponse> addQuestionToExam(
            @PathVariable UUID examId,
            @RequestBody QuestionRequest request) {
        return new ResponseEntity<>(questionService.createQuestion(examId, request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER', 'TEACHER', 'ROLE_STUDENT', 'STUDENT', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<QuestionResponse>> getQuestionsForExam(@PathVariable UUID examId) {
        return ResponseEntity.ok(questionService.getQuestionsForExam(examId));
    }
}