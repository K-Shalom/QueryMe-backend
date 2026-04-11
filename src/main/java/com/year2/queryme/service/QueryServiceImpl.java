package com.year2.queryme.service;

import com.year2.queryme.model.AnswerKey;
import com.year2.queryme.model.Question;
import com.year2.queryme.model.Submission;
import com.year2.queryme.model.dto.SubmissionRequest;
import com.year2.queryme.model.dto.SubmissionResponse;
import com.year2.queryme.repository.AnswerKeyRepository;
import com.year2.queryme.repository.QuestionRepository;
import com.year2.queryme.repository.SubmissionRepository;
import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
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

    @Override
    @Transactional
    public SubmissionResponse submitQuery(SubmissionRequest request) {
        log.info("Processing submission for student {} question {}", request.getStudentId(), request.getQuestionId());
        
        Question question = questionRepository.findById(request.getQuestionId())
            .orElseThrow(() -> new IllegalArgumentException("Question not found"));
            
        AnswerKey answerKey = answerKeyRepository.findByQuestionId(request.getQuestionId())
            .orElseThrow(() -> new IllegalArgumentException("AnswerKey not found for question"));

        Submission.SubmissionBuilder submissionBuilder = Submission.builder()
            .studentId(request.getStudentId())
            .examId(request.getExamId())
            .questionId(request.getQuestionId())
            .submittedQuery(request.getQuery())
            .isCorrect(false)
            .score(0);

        try {
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
            
        } catch (IllegalArgumentException e) {
            log.warn("Query Validation Error", e);
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
            .isCorrect(submission.getIsCorrect())
            .score(submission.getScore())
            .executionError(submission.getExecutionError())
            .build();
    }
}
