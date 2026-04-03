package com.year2.queryme.controller;

import com.year2.queryme.model.dto.QuestionRequest;
import com.year2.queryme.model.dto.QuestionResponse;
import com.year2.queryme.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exams/{examId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    // @PreAuthorize("hasRole('TEACHER')") // <-- COMMENTED OUT FOR TESTING
    public ResponseEntity<QuestionResponse> addQuestionToExam(
            @PathVariable UUID examId,
            @Valid @RequestBody QuestionRequest request) {

        QuestionResponse response = questionService.createQuestion(examId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    // @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')") // <-- COMMENTED OUT FOR TESTING
    public ResponseEntity<List<QuestionResponse>> getQuestionsForExam(@PathVariable UUID examId) {
        return ResponseEntity.ok(questionService.getQuestionsForExam(examId));
    }
}