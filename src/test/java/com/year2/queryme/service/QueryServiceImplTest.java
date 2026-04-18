package com.year2.queryme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.year2.queryme.model.AnswerKey;
import com.year2.queryme.model.Exam;
import com.year2.queryme.model.ExamSession;
import com.year2.queryme.model.Question;
import com.year2.queryme.model.Submission;
import com.year2.queryme.model.dto.SubmissionRequest;
import com.year2.queryme.model.dto.SubmissionResponse;
import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.model.enums.VisibilityMode;
import com.year2.queryme.repository.AnswerKeyRepository;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.ExamSessionRepository;
import com.year2.queryme.repository.QuestionRepository;
import com.year2.queryme.repository.SubmissionRepository;
import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
import com.year2.queryme.sandbox.service.SandboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceImplTest {

    @Mock
    private QueryValidator queryValidator;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private ResultSetComparator resultSetComparator;

    @Mock
    private SandboxService sandboxService;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AnswerKeyRepository answerKeyRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ResultService resultService;

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private CurrentUserService currentUserService;

        @Mock
        private SqlDialectAdapter sqlDialectAdapter;

    private QueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        queryService = new QueryServiceImpl(
                queryValidator,
                queryExecutor,
                resultSetComparator,
                sandboxService,
                submissionRepository,
                answerKeyRepository,
                questionRepository,
                resultService,
                examSessionRepository,
                examRepository,
                new ObjectMapper(),
                currentUserService,
                sqlDialectAdapter
        );
    }

    @Test
    void submitQueryReturnsExecutionErrorWithoutRollingBackSubmissionPersistence() {
        UUID examId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        String schemaName = "exam_12345678_student_87654321";

        SubmissionRequest request = new SubmissionRequest();
        request.setExamId(examId);
        request.setQuestionId(questionId);
        request.setStudentId(studentId);
        request.setSessionId(sessionId);
        request.setQuery("SELECT * FROM salary;");

        Question question = Question.builder()
                .id(questionId)
                .examId(examId)
                .marks(10)
                .orderSensitive(false)
                .partialMarks(false)
                .build();

        AnswerKey answerKey = AnswerKey.builder()
                .questionId(questionId)
                .expectedColumns("[\"id\"]")
                .expectedRows("[]")
                .build();

        Exam exam = Exam.builder()
                .id(examId.toString())
                .courseId("course-1")
                .title("SQL Exam")
                .status(ExamStatus.PUBLISHED)
                .visibilityMode(VisibilityMode.IMMEDIATE)
                .seedSql("CREATE TABLE salary(id INT);")
                .build();

        ExamSession session = ExamSession.builder()
                .id(sessionId.toString())
                .examId(examId.toString())
                .studentId(studentId.toString())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(answerKeyRepository.findByQuestionId(questionId)).thenReturn(Optional.of(answerKey));
        when(examRepository.findById(examId.toString())).thenReturn(Optional.of(exam));
        when(currentUserService.hasRole(com.year2.queryme.model.enums.UserTypes.STUDENT)).thenReturn(true);
        when(currentUserService.requireCurrentUserId()).thenReturn(studentId);
        when(examSessionRepository.findById(sessionId.toString())).thenReturn(Optional.of(session));
        when(sandboxService.getSandboxConnectionDetails(examId, studentId))
                .thenReturn(new SandboxConnectionInfo(schemaName, "sandbox_user"));
        when(sqlDialectAdapter.adaptForExecution(request.getQuery())).thenReturn(request.getQuery());
        when(sqlDialectAdapter.ensureFinalStatementReturnsRows(request.getQuery())).thenReturn(request.getQuery());
        doNothing().when(queryValidator).validate(request.getQuery(), schemaName, false);
        when(queryExecutor.executeSandboxedScript(schemaName, request.getQuery(), 10, false))
                .thenThrow(new RuntimeException("relation \"salary\" does not exist"));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(submissionId);
            return submission;
        });

        SubmissionResponse response = queryService.submitQuery(request);

        assertNotNull(response.getSubmissionId());
        assertEquals(submissionId, response.getSubmissionId());
        assertEquals(sessionId, response.getSessionId());
        assertEquals(Boolean.FALSE, response.getIsCorrect());
        assertEquals(0, response.getScore());
        assertTrue(response.getExecutionError().contains("Execution Error:"));
        assertTrue(response.getExecutionError().contains("salary"));
        assertEquals(Boolean.TRUE, response.getResultsVisible());
        verify(submissionRepository).save(any(Submission.class));
        verify(resultService).processNewSubmission(any(Submission.class), eq(question));
    }

    @Test
    void submitQueryHidesExecutionErrorForStudentsWhenResultsAreDeferred() {
        UUID examId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        String schemaName = "exam_12345678_student_87654321";

        SubmissionRequest request = new SubmissionRequest();
        request.setExamId(examId);
        request.setQuestionId(questionId);
        request.setStudentId(studentId);
        request.setSessionId(sessionId);
        request.setQuery("SELECT * FROM salary;");

        Question question = Question.builder()
                .id(questionId)
                .examId(examId)
                .marks(10)
                .orderSensitive(false)
                .partialMarks(false)
                .build();

        AnswerKey answerKey = AnswerKey.builder()
                .questionId(questionId)
                .expectedColumns("[\"id\"]")
                .expectedRows("[]")
                .build();

        Exam exam = Exam.builder()
                .id(examId.toString())
                .courseId("course-1")
                .title("SQL Exam")
                .status(ExamStatus.PUBLISHED)
                .visibilityMode(VisibilityMode.END_OF_EXAM)
                .seedSql("CREATE TABLE salary(id INT);")
                .build();

        ExamSession session = ExamSession.builder()
                .id(sessionId.toString())
                .examId(examId.toString())
                .studentId(studentId.toString())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(answerKeyRepository.findByQuestionId(questionId)).thenReturn(Optional.of(answerKey));
        when(examRepository.findById(examId.toString())).thenReturn(Optional.of(exam));
        when(currentUserService.hasRole(com.year2.queryme.model.enums.UserTypes.STUDENT)).thenReturn(true);
        when(currentUserService.requireCurrentUserId()).thenReturn(studentId);
        when(examSessionRepository.findById(sessionId.toString())).thenReturn(Optional.of(session));
        when(sandboxService.getSandboxConnectionDetails(examId, studentId))
                .thenReturn(new SandboxConnectionInfo(schemaName, "sandbox_user"));
        when(sqlDialectAdapter.adaptForExecution(request.getQuery())).thenReturn(request.getQuery());
        when(sqlDialectAdapter.ensureFinalStatementReturnsRows(request.getQuery())).thenReturn(request.getQuery());
        doNothing().when(queryValidator).validate(request.getQuery(), schemaName, false);
        when(queryExecutor.executeSandboxedScript(schemaName, request.getQuery(), 10, false))
                .thenThrow(new RuntimeException("relation \"salary\" does not exist"));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(submissionId);
            return submission;
        });

        SubmissionResponse response = queryService.submitQuery(request);

        assertNotNull(response.getSubmissionId());
        assertEquals(submissionId, response.getSubmissionId());
        assertEquals(Boolean.FALSE, response.getResultsVisible());
        assertNull(response.getExecutionError());
        assertNull(response.getScore());
        assertNull(response.getIsCorrect());
        verify(submissionRepository).save(any(Submission.class));
                verify(resultService).processNewSubmission(any(Submission.class), eq(question));
    }
}
