package com.year2.queryme.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.year2.queryme.model.AnswerKey;
import com.year2.queryme.model.Exam;
import com.year2.queryme.model.Question;
import com.year2.queryme.model.User;
import com.year2.queryme.model.dto.QuestionRequest;
import com.year2.queryme.model.dto.QuestionResponse;
import com.year2.queryme.model.dto.AnswerKeyDto;
import com.year2.queryme.repository.AnswerKeyRepository;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.QuestionRepository;
import com.year2.queryme.repository.UserRepository;
import com.year2.queryme.sandbox.service.SandboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerKeyRepository answerKeyRepository;
    private final ExamRepository examRepository;
    private final SandboxService sandboxService;
    private final QueryExecutor queryExecutor;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public QuestionResponse createQuestion(UUID examId, QuestionRequest request) {
        Question question = Question.builder()
                .examId(examId)
                .prompt(request.getPrompt())
                .referenceQuery(request.getReferenceQuery())
                .marks(request.getMarks())
                .orderIndex(request.getOrderIndex())
                .orderSensitive(request.getOrderSensitive())
                .partialMarks(request.getPartialMarks())
                .build();

        Question savedQuestion = questionRepository.save(question);
        generateAndSaveAnswerKey(savedQuestion.getId(), examId, request.getReferenceQuery());

        return mapToResponse(savedQuestion);
    }

    @Override
    public List<QuestionResponse> getQuestionsForExam(UUID examId) {
        return questionRepository.findByExamIdOrderByOrderIndexAsc(examId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AnswerKeyDto getAnswerKeyForQuestion(UUID questionId) {
        AnswerKey answerKey = answerKeyRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new RuntimeException("Answer key not found for question ID: " + questionId));

        return AnswerKeyDto.builder()
                .id(answerKey.getId())
                .questionId(answerKey.getQuestionId())
                .expectedColumns(answerKey.getExpectedColumns())
                .expectedRows(answerKey.getExpectedRows())
                .build();
    }

    private void generateAndSaveAnswerKey(UUID questionId, UUID examId, String referenceQuery) {
        Exam exam = examRepository.findById(examId.toString())
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        List<User> allUsers = userRepository.findAll();
        if (allUsers.isEmpty()) {
            throw new RuntimeException("No users found to provision sandbox");
        }
        UUID realTeacherUserId = UUID.fromString(allUsers.get(0).getId().toString());

        String schemaName = null;

        try {
            schemaName = sandboxService.provisionSandbox(examId, realTeacherUserId, exam.getSeedSql());
            List<Map<String, Object>> rows = queryExecutor.executeSandboxedQuery(schemaName, referenceQuery, 5);

            List<String> columns = new ArrayList<>();
            if (!rows.isEmpty()) {
                columns.addAll(rows.get(0).keySet());
            }

            String expectedColumnsJson = objectMapper.writeValueAsString(columns);
            String expectedRowsJson = objectMapper.writeValueAsString(rows);

            AnswerKey answerKey = AnswerKey.builder()
                    .questionId(questionId)
                    .expectedColumns(expectedColumnsJson)
                    .expectedRows(expectedRowsJson)
                    .build();

            answerKeyRepository.save(answerKey);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process answer key JSON", e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing teacher's reference query: " + e.getMessage(), e);
        } finally {
            if (schemaName != null) {
                sandboxService.teardownSandbox(examId, realTeacherUserId);
            }
        }
    }

    private QuestionResponse mapToResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setExamId(question.getExamId());
        response.setPrompt(question.getPrompt());
        response.setReferenceQuery(question.getReferenceQuery());
        response.setMarks(question.getMarks());
        response.setOrderIndex(question.getOrderIndex());
        response.setOrderSensitive(question.getOrderSensitive());
        response.setPartialMarks(question.getPartialMarks());
        return response;
    }
}