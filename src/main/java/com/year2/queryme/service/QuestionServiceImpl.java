package com.year2.queryme.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.year2.queryme.model.AnswerKey;
import com.year2.queryme.model.Exam;
import com.year2.queryme.model.Question;
import com.year2.queryme.model.Student;
import com.year2.queryme.model.dto.QuestionRequest;
import com.year2.queryme.model.dto.QuestionResponse;
import com.year2.queryme.model.dto.AnswerKeyDto;
import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.model.enums.UserTypes;
import com.year2.queryme.repository.AnswerKeyRepository;
import com.year2.queryme.repository.CourseEnrollmentRepository;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.QuestionRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.sandbox.service.SandboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerKeyRepository answerKeyRepository;
    private final ExamRepository examRepository;
    private final SandboxService sandboxService;
    private final QueryExecutor queryExecutor;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final StudentRepository studentRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    @Override
    @Transactional
    public QuestionResponse createQuestion(UUID examId, QuestionRequest request) {
        Question savedQuestion = questionRepository.save(buildQuestion(examId, request, null));
        generateAndSaveAnswerKey(savedQuestion.getId(), examId, request.getReferenceQuery());

        return mapToResponse(savedQuestion, true);
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(UUID examId, UUID questionId, QuestionRequest request) {
        Question existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found: " + questionId));

        if (!existingQuestion.getExamId().equals(examId)) {
            throw new RuntimeException("Question does not belong to exam: " + examId);
        }

        Question savedQuestion = questionRepository.save(buildQuestion(examId, request, existingQuestion));
        generateAndSaveAnswerKey(savedQuestion.getId(), examId, request.getReferenceQuery());

        return mapToResponse(savedQuestion, true);
    }

    @Override
    public List<QuestionResponse> getQuestionsForExam(UUID examId) {
        boolean includeReferenceQuery = !currentUserService.hasRole(UserTypes.STUDENT);

        if (currentUserService.hasRole(UserTypes.STUDENT)) {
            assertCurrentStudentCanAccessExam(examId);
        }

        return questionRepository.findByExamIdOrderByOrderIndexAsc(examId)
                .stream()
                .map(question -> mapToResponse(question, includeReferenceQuery))
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

        UUID realTeacherUserId = currentUserService.requireCurrentUserId();

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

            AnswerKey answerKey = answerKeyRepository.findByQuestionId(questionId)
                    .orElse(AnswerKey.builder().questionId(questionId).build());
            answerKey.setExpectedColumns(expectedColumnsJson);
            answerKey.setExpectedRows(expectedRowsJson);

            answerKeyRepository.save(answerKey);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process answer key JSON", e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing teacher's reference query: " + e.getMessage(), e);
        } finally {
            if (schemaName != null) {
                try {
                    sandboxService.teardownSandbox(examId, realTeacherUserId);
                } catch (RuntimeException cleanupException) {
                    log.warn("Sandbox cleanup skipped for question {} in exam {}: {}",
                            questionId, examId, cleanupException.getMessage());
                }
            }
        }
    }

    private Question buildQuestion(UUID examId, QuestionRequest request, Question existingQuestion) {
        Question.QuestionBuilder builder = Question.builder()
                .examId(examId)
                .prompt(request.getPrompt())
                .referenceQuery(request.getReferenceQuery())
                .marks(request.getMarks())
                .orderIndex(request.getOrderIndex())
                .orderSensitive(request.getOrderSensitive())
                .partialMarks(request.getPartialMarks());

        if (existingQuestion != null) {
            builder.id(existingQuestion.getId())
                    .createdAt(existingQuestion.getCreatedAt());
        }

        return builder.build();
    }

    private QuestionResponse mapToResponse(Question question, boolean includeReferenceQuery) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setExamId(question.getExamId());
        response.setPrompt(question.getPrompt());
        response.setReferenceQuery(includeReferenceQuery ? question.getReferenceQuery() : null);
        response.setMarks(question.getMarks());
        response.setOrderIndex(question.getOrderIndex());
        response.setOrderSensitive(question.getOrderSensitive());
        response.setPartialMarks(question.getPartialMarks());
        return response;
    }

    private void assertCurrentStudentCanAccessExam(UUID examId) {
        Exam exam = examRepository.findById(examId.toString())
                .orElseThrow(() -> new RuntimeException("Exam not found: " + examId));

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("Exam is not published");
        }

        Student student = studentRepository.findByUser_Id(currentUserService.requireCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        if (student.getCourse() != null && Objects.equals(student.getCourse().getId().toString(), exam.getCourseId())) {
            return;
        }

        Set<String> enrolledCourseIds = courseEnrollmentRepository.findByStudentId(student.getId())
                .stream()
                .map(enrollment -> enrollment.getCourse().getId().toString())
                .collect(Collectors.toSet());

        if (!enrolledCourseIds.contains(exam.getCourseId())) {
            throw new RuntimeException("Student is not assigned to this exam");
        }
    }
}
