package com.year2.queryme.service;

import com.year2.queryme.model.Exam;
import com.year2.queryme.model.ExamSession;
import com.year2.queryme.model.Question;
import com.year2.queryme.model.Result;
import com.year2.queryme.model.Submission;
import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.model.enums.VisibilityMode;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.ExamSessionRepository;
import com.year2.queryme.repository.QuestionRepository;
import com.year2.queryme.repository.ResultRepository;
import com.year2.queryme.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultServiceImpl implements ResultService {

    private final ResultRepository resultRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final SubmissionRepository submissionRepository;
    private final QuestionRepository questionRepository;

    /**
     * Applies the exam's current visibility mode before exposing session results.
     */

    @Override
    public List<Result> getResultsForStudent(UUID sessionId) {
        ExamSession session = examSessionRepository.findById(sessionId.toString())
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        Exam exam = examRepository.findById(session.getExamId())
                .orElseThrow(() -> new RuntimeException("Exam not found: " + session.getExamId()));

        if (exam.getVisibilityMode() == VisibilityMode.NEVER) {
            return List.of();
        }

        if (exam.getVisibilityMode() == VisibilityMode.END_OF_EXAM
                && exam.getStatus() != ExamStatus.CLOSED) {
            return List.of();
        }

        return resultRepository.findBySessionId(sessionId);
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
    public List<Result> getResultsForTeacher(UUID examId) {
        return resultRepository.findAllByExamId(examId);
    }

    @Override
    public Result saveQueryResult(UUID submissionId, Integer score, Boolean isCorrect) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        ExamSession session = examSessionRepository.findByExamIdAndStudentId(
                        submission.getExamId().toString(),
                        submission.getStudentId().toString())
                .orElseThrow(() -> new RuntimeException(
                        "Session not found for exam %s and student %s"
                                .formatted(submission.getExamId(), submission.getStudentId())));

        Question question = questionRepository.findById(submission.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found: " + submission.getQuestionId()));

        Result result = resultRepository.findBySubmissionId(submissionId)
                .orElseGet(Result::new);

        result.setSubmissionId(submissionId);
        result.setQuestionId(submission.getQuestionId());
        result.setSessionId(UUID.fromString(session.getId()));
        result.setExamId(submission.getExamId());
        result.setScore(score);
        result.setMaxScore(question.getMarks());
        result.setIsCorrect(Boolean.TRUE.equals(isCorrect));
        result.setGradedAt(LocalDateTime.now());

        return resultRepository.save(result);
    }
}
