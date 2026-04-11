package com.year2.queryme.service;

import com.year2.queryme.model.AnswerKey;
import com.year2.queryme.model.Exam;
import com.year2.queryme.model.ExamSession;
import com.year2.queryme.model.Question;
import com.year2.queryme.model.Submission;
import com.year2.queryme.model.dto.SubmissionRequest;
import com.year2.queryme.model.dto.SubmissionResponse;
import com.year2.queryme.model.enums.VisibilityMode;
import com.year2.queryme.repository.AnswerKeyRepository;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.ExamSessionRepository;
import com.year2.queryme.repository.QuestionRepository;
import com.year2.queryme.repository.SubmissionRepository;
import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.year2.queryme.sandbox.service.SandboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private final QueryValidator queryValidator;
    private final QueryExecutor queryExecutor;
    private final ResultSetComparator resultSetComparator;
    private final SandboxService sandboxService;
    private final SubmissionRepository submissionRepository;
    private final AnswerKeyRepository answerKeyRepository;
    private final QuestionRepository questionRepository;
    private final ResultService resultService;
    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public SubmissionResponse submitQuery(SubmissionRequest request) {
        log.info("Processing submission for student {} question {}", request.getStudentId(), request.getQuestionId());
        Submission.SubmissionBuilder submissionBuilder = Submission.builder()
            .studentId(request.getStudentId())
            .examId(request.getExamId())
            .questionId(request.getQuestionId())
            .submittedQuery(request.getQuery())
            .isCorrect(false)
            .score(0);

        Question question = null;
        AnswerKey answerKey = null;
        Exam exam = null;
        boolean persistSubmission = false;

        try {
            question = questionRepository.findById(request.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("Question not found"));

            answerKey = answerKeyRepository.findByQuestionId(request.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("AnswerKey not found for question"));

            if (!question.getExamId().equals(request.getExamId())) {
                throw new IllegalArgumentException("Question does not belong to the supplied exam");
            }

            exam = examRepository.findById(request.getExamId().toString())
                    .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

            if (currentUserService.hasRole(com.year2.queryme.model.enums.UserTypes.STUDENT)
                    && !currentUserService.requireCurrentUserId().equals(request.getStudentId())) {
                throw new IllegalArgumentException("Students can only submit queries for themselves");
            }

            ExamSession activeSession = resolveActiveSession(request);
            submissionBuilder.sessionId(java.util.UUID.fromString(activeSession.getId()));
            persistSubmission = true;

            // 1. Validate blocklist
            queryValidator.validate(request.getQuery());

            // 2. Get Sandbox connection details
            SandboxConnectionInfo sandboxInfo = sandboxService.getSandboxConnectionDetails(
                request.getExamId(), request.getStudentId());

            // 3. Executed Sandboxed Query
            List<Map<String, Object>> studentResult = queryExecutor.executeSandboxedQuery(
                sandboxInfo.schemaName(), request.getQuery(), 10); // 10s hard timeout
                
            // 4. Compare ResultSets
            Boolean orderSensitive = question.getOrderSensitive() != null ? question.getOrderSensitive() : false;
            boolean isCorrect = resultSetComparator.compare(studentResult, answerKey.getExpectedRows(), orderSensitive);
            List<String> resultColumns = studentResult.isEmpty()
                    ? List.of()
                    : List.copyOf(studentResult.get(0).keySet());
            
            int finalScore = 0;
            if (isCorrect) {
                finalScore = question.getMarks();
            } else if (Boolean.TRUE.equals(question.getPartialMarks())) {
                // PARTIAL MARKS logic: if result count matches, give 50%
                List<Map<String, Object>> expectedData = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(answerKey.getExpectedRows(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
                if (studentResult.size() == expectedData.size()) {
                    finalScore = question.getMarks() / 2;
                    submissionBuilder.executionError("Partial Credit: Row count matches, but data is incorrect.");
                }
            }
            
            submissionBuilder.isCorrect(isCorrect);
            submissionBuilder.score(finalScore);
            submissionBuilder.resultColumns(objectMapper.writeValueAsString(resultColumns));
            submissionBuilder.resultRows(objectMapper.writeValueAsString(studentResult));
            
        } catch (IllegalArgumentException e) {
            log.warn("Query Validation Error", e);
            if (!persistSubmission) {
                return SubmissionResponse.builder()
                        .executionError("Validation Error: " + e.getMessage())
                        .resultsVisible(false)
                        .build();
            }
            submissionBuilder.executionError("Validation Error: " + e.getMessage());
        } catch (QueryTimeoutException e) {
            log.error("Query Timeout Error", e);
            submissionBuilder.executionError("Timeout Error: Query exceeded 10s execution limit.");
        } catch (Exception e) {
            log.error("Query Execution/Comparison Error", e);
            submissionBuilder.executionError("Execution Error: " + e.getMessage());
        }

        // 5. Save the Submission
        Submission submission = submissionRepository.save(submissionBuilder.build());
        resultService.processNewSubmission(submission.getId());

        return SubmissionResponse.builder()
            .submissionId(submission.getId())
            .sessionId(submission.getSessionId())
            .isCorrect(exam.getVisibilityMode() == VisibilityMode.IMMEDIATE ? submission.getIsCorrect() : null)
            .score(exam.getVisibilityMode() == VisibilityMode.IMMEDIATE ? submission.getScore() : null)
            .executionError(submission.getExecutionError())
            .resultsVisible(exam.getVisibilityMode() == VisibilityMode.IMMEDIATE)
            .resultColumns(exam.getVisibilityMode() == VisibilityMode.IMMEDIATE
                    ? parseColumns(submission.getResultColumns()) : null)
            .resultRows(exam.getVisibilityMode() == VisibilityMode.IMMEDIATE
                    ? parseRows(submission.getResultRows()) : null)
            .build();
    }

    private ExamSession resolveActiveSession(SubmissionRequest request) {
        if (request.getSessionId() != null) {
            ExamSession session = examSessionRepository.findById(request.getSessionId().toString())
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));
            validateSession(session, request);
            return session;
        }

        ExamSession session = examSessionRepository
                .findFirstByExamIdAndStudentIdAndSubmittedAtIsNullOrderByStartedAtDesc(
                        request.getExamId().toString(),
                        request.getStudentId().toString())
                .orElseThrow(() -> new IllegalArgumentException("No active session found for this exam"));
        validateSession(session, request);
        return session;
    }

    private void validateSession(ExamSession session, SubmissionRequest request) {
        if (!session.getExamId().equals(request.getExamId().toString())
                || !session.getStudentId().equals(request.getStudentId().toString())) {
            throw new IllegalArgumentException("Session does not belong to the supplied exam/student");
        }

        if (session.getSubmittedAt() != null) {
            throw new IllegalArgumentException("Session is already submitted");
        }

        if (session.getExpiresAt() != null && java.time.LocalDateTime.now().isAfter(session.getExpiresAt())) {
            throw new IllegalArgumentException("Session has expired");
        }
    }

    private List<String> parseColumns(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Failed to parse submission result columns: {}", ex.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> parseRows(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Failed to parse submission result rows: {}", ex.getMessage());
            return null;
        }
    }
}
