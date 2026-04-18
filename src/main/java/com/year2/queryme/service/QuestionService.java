package com.year2.queryme.service;

import com.year2.queryme.model.dto.QuestionRequest;
import com.year2.queryme.model.dto.QuestionResponse;
import com.year2.queryme.model.dto.AnswerKeyDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface QuestionService {
    QuestionResponse createQuestion(UUID examId, QuestionRequest request);
    QuestionResponse updateQuestion(UUID examId, UUID questionId, QuestionRequest request);
    Page<QuestionResponse> getQuestionsForExam(UUID examId, Pageable pageable);
    AnswerKeyDto getAnswerKeyForQuestion(UUID questionId);
}
