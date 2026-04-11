package com.year2.queryme.service;

import com.year2.queryme.model.Exam;
import com.year2.queryme.model.ExamSession;
import com.year2.queryme.model.Question;
import com.year2.queryme.model.Result;
import com.year2.queryme.model.Submission;
import com.year2.queryme.model.Student;
import com.year2.queryme.model.dto.StudentExamResultDto;
import com.year2.queryme.model.dto.StudentQuestionResultDto;
import com.year2.queryme.model.dto.TeacherDashboardRowDto;
import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.model.enums.UserTypes;
import com.year2.queryme.model.enums.VisibilityMode;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.ExamSessionRepository;
import com.year2.queryme.repository.QuestionRepository;
import com.year2.queryme.repository.ResultRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.repository.SubmissionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultServiceImpl implements ResultService {

    private final ResultRepository resultRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final SubmissionRepository submissionRepository;
    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    /**
     * Applies the exam's current visibility mode before exposing session results.
     */

    @Override
    public StudentExamResultDto getResultsForStudent(UUID sessionId) {
        ExamSession session = examSessionRepository.findById(sessionId.toString())
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        if (currentUserService.hasRole(UserTypes.STUDENT)
                && !session.getStudentId().equals(currentUserService.requireCurrentUserId().toString())) {
            throw new RuntimeException("Students can only view their own results");
        }

        Exam exam = examRepository.findById(session.getExamId())
                .orElseThrow(() -> new RuntimeException("Exam not found: " + session.getExamId()));

        boolean visible = isVisibleToStudent(exam, session);
        List<Question> questions = questionRepository.findByExamIdOrderByOrderIndexAsc(UUID.fromString(session.getExamId()));
        Map<UUID, Submission> latestSubmissions = latestSubmissionByQuestion(
                submissionRepository.findBySessionIdOrderBySubmittedAtDesc(sessionId));

        List<StudentQuestionResultDto> questionResults = new ArrayList<>();
        int totalScore = 0;
        int totalMaxScore = 0;

        for (Question question : questions) {
            Submission submission = latestSubmissions.get(question.getId());
            totalMaxScore += question.getMarks();
            if (visible && submission != null && submission.getScore() != null) {
                totalScore += submission.getScore();
            }

            questionResults.add(StudentQuestionResultDto.builder()
                    .questionId(question.getId())
                    .prompt(question.getPrompt())
                    .submittedQuery(submission != null ? submission.getSubmittedQuery() : null)
                    .score(visible && submission != null ? submission.getScore() : null)
                    .maxScore(visible ? question.getMarks() : null)
                    .isCorrect(visible && submission != null ? submission.getIsCorrect() : null)
                    .submittedAt(submission != null ? submission.getSubmittedAt() : null)
                    .resultColumns(visible && submission != null ? parseColumns(submission.getResultColumns()) : null)
                    .resultRows(visible && submission != null ? parseRows(submission.getResultRows()) : null)
                    .build());
        }

        return StudentExamResultDto.builder()
                .sessionId(sessionId)
                .examId(UUID.fromString(session.getExamId()))
                .studentId(UUID.fromString(session.getStudentId()))
                .visibilityMode(exam.getVisibilityMode())
                .visible(visible)
                .totalScore(visible ? totalScore : null)
                .totalMaxScore(visible ? totalMaxScore : null)
                .questions(questionResults)
                .build();
    }

    @Override
    public void processNewSubmission(UUID submissionId) {
        try {
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

            saveQueryResult(submissionId, submission.getScore(), submission.getIsCorrect());
        } catch (RuntimeException ex) {
            log.warn("Could not synchronize submission {} into results yet: {}", submissionId, ex.getMessage());
        }
    }

    @Override
    public List<TeacherDashboardRowDto> getResultsForTeacher(UUID examId) {
        Map<String, Submission> latestSubmissions = new LinkedHashMap<>();
        for (Submission submission : submissionRepository.findByExamIdOrderBySubmittedAtDesc(examId)) {
            String key = submission.getStudentId() + ":" + submission.getQuestionId();
            latestSubmissions.putIfAbsent(key, submission);
        }

        Map<UUID, Question> questionMap = questionRepository.findAll().stream()
                .collect(Collectors.toMap(Question::getId, question -> question));

        return latestSubmissions.values().stream()
                .map(submission -> {
                    Student student = studentRepository.findByUser_Id(submission.getStudentId()).orElse(null);
                    Question question = questionMap.get(submission.getQuestionId());
                    return TeacherDashboardRowDto.builder()
                            .studentId(submission.getStudentId())
                            .studentName(student != null ? student.getFullName() : submission.getStudentId().toString())
                            .sessionId(submission.getSessionId())
                            .questionId(submission.getQuestionId())
                            .questionPrompt(question != null ? question.getPrompt() : null)
                            .score(submission.getScore())
                            .maxScore(question != null ? question.getMarks() : null)
                            .isCorrect(submission.getIsCorrect())
                            .submittedQuery(submission.getSubmittedQuery())
                            .submittedAt(submission.getSubmittedAt())
                            .build();
                })
                .toList();
    }

    @Override
    public Result saveQueryResult(UUID submissionId, Integer score, Boolean isCorrect) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        Question question = questionRepository.findById(submission.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found: " + submission.getQuestionId()));

        Result result = resultRepository.findBySubmissionId(submissionId)
                .orElseGet(Result::new);

        result.setSubmissionId(submissionId);
        result.setQuestionId(submission.getQuestionId());
        result.setSessionId(submission.getSessionId());
        result.setExamId(submission.getExamId());
        result.setScore(score);
        result.setMaxScore(question.getMarks());
        result.setIsCorrect(Boolean.TRUE.equals(isCorrect));
        result.setGradedAt(LocalDateTime.now());

        return resultRepository.save(result);
    }

    private boolean isVisibleToStudent(Exam exam, ExamSession session) {
        if (exam.getVisibilityMode() == VisibilityMode.NEVER) {
            return false;
        }

        if (exam.getVisibilityMode() == VisibilityMode.END_OF_EXAM) {
            return session.getSubmittedAt() != null
                    || (session.getExpiresAt() != null && LocalDateTime.now().isAfter(session.getExpiresAt()))
                    || exam.getStatus() == ExamStatus.CLOSED;
        }

        return true;
    }

    private Map<UUID, Submission> latestSubmissionByQuestion(List<Submission> submissions) {
        Map<UUID, Submission> latest = new LinkedHashMap<>();
        for (Submission submission : submissions) {
            latest.putIfAbsent(submission.getQuestionId(), submission);
        }
        return latest;
    }

    private List<String> parseColumns(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Failed to parse result columns JSON: {}", ex.getMessage());
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
            log.warn("Failed to parse result rows JSON: {}", ex.getMessage());
            return null;
        }
    }
}
