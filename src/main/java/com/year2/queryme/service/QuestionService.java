package com.year2.queryme.service;

import com.year2.queryme.model.dto.QuestionRequest;
import com.year2.queryme.model.dto.QuestionResponse;
import com.year2.queryme.model.dto.AnswerKeyDto;

import java.util.List;
import java.util.UUID;

public interface QuestionService {
    QuestionResponse createQuestion(UUID examId, QuestionRequest request);
    List<QuestionResponse> getQuestionsForExam(UUID examId);
    AnswerKeyDto getAnswerKeyForQuestion(UUID questionId);
}