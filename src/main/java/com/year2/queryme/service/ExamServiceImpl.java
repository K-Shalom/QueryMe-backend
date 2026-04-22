package com.year2.queryme.service;

import com.year2.queryme.model.Exam;
import com.year2.queryme.model.Student;
import com.year2.queryme.model.dto.*;
import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.model.enums.UserTypes;
import com.year2.queryme.model.mapper.ExamMapper;
import com.year2.queryme.repository.CourseEnrollmentRepository;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.QuestionRepository;
import com.year2.queryme.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final CurrentUserService currentUserService;
    private final StudentRepository studentRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final QuestionRepository questionRepository;
    private final com.year2.queryme.repository.AnswerKeyRepository answerKeyRepository;
    private final com.year2.queryme.repository.CourseRepository courseRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ExamResponse cloneExam(String examId) {
        Exam originalExam = findById(examId);
        assertTeacherOwnsCourse(originalExam.getCourseId());

        Exam clonedExam = Exam.builder()
                .courseId(originalExam.getCourseId())
                .title(originalExam.getTitle() + " (Copy)")
                .description(originalExam.getDescription())
                .visibilityMode(originalExam.getVisibilityMode())
                .timeLimitMins(originalExam.getTimeLimitMins())
                .maxAttempts(originalExam.getMaxAttempts())
                .seedSql(originalExam.getSeedSql())
                .build();

        Exam savedClonedExam = examRepository.save(clonedExam);

        List<com.year2.queryme.model.Question> originalQuestions = questionRepository.findByExamIdOrderByOrderIndexAsc(java.util.UUID.fromString(examId));

        for (com.year2.queryme.model.Question q : originalQuestions) {
            com.year2.queryme.model.Question clonedQ = com.year2.queryme.model.Question.builder()
                    .examId(java.util.UUID.fromString(savedClonedExam.getId()))
                    .prompt(q.getPrompt())
                    .referenceQuery(q.getReferenceQuery())
                    .marks(q.getMarks())
                    .orderIndex(q.getOrderIndex())
                    .orderSensitive(q.getOrderSensitive())
                    .partialMarks(q.getPartialMarks())
                    .build();
            
            com.year2.queryme.model.Question savedClonedQ = questionRepository.save(clonedQ);

            answerKeyRepository.findByQuestionId(q.getId()).ifPresent(ak -> {
                com.year2.queryme.model.AnswerKey clonedAk = com.year2.queryme.model.AnswerKey.builder()
                        .questionId(savedClonedQ.getId())
                        .expectedColumns(ak.getExpectedColumns())
                        .expectedRows(ak.getExpectedRows())
                        .build();
                answerKeyRepository.save(clonedAk);
            });
        }

        return toResponse(savedClonedExam);
    }

    @Override
    public ExamResponse createExam(CreateExamRequest request) {
        assertTeacherOwnsCourse(request.getCourseId());
        if (request.getSeedSql() == null || request.getSeedSql().isBlank()) {
            throw new RuntimeException("seed_sql is required — Group D needs it to create sandboxes");
        }
        if (request.getVisibilityMode() == null) {
            throw new RuntimeException("visibility_mode is required");
        }

        Exam exam = Exam.builder()
                .courseId(request.getCourseId())
                .title(request.getTitle())
                .description(request.getDescription())
                .visibilityMode(request.getVisibilityMode())
                .timeLimitMins(request.getTimeLimitMins())
                .maxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 1)
                .seedSql(request.getSeedSql())
                .build();

        return toResponse(examRepository.save(exam));
    }

    @Override
    public ExamResponse getExamById(String examId) {
        Exam exam = findById(examId);
        assertCurrentUserCanAccessExam(exam);
        return toResponse(exam);
    }

    @Override
    public List<ExamResponse> getExamsByCourse(String courseId) {
        if (currentUserService.hasRole(UserTypes.TEACHER)) {
            assertTeacherOwnsCourse(courseId);
        }
        List<Exam> exams = currentUserService.hasRole(UserTypes.STUDENT)
                ? examRepository.findByCourseIdAndStatus(courseId, ExamStatus.PUBLISHED)
                : examRepository.findByCourseId(courseId);

        return toResponsesForCurrentUser(exams);
    }

    @Override
    public List<ExamResponse> getPublishedExams() {
        return toResponsesForCurrentUser(examRepository.findByStatus(ExamStatus.PUBLISHED));
    }

    @Override
    public ExamResponse updateExam(String examId, UpdateExamRequest request) {
        Exam exam = findById(examId);
        assertTeacherOwnsCourse(exam.getCourseId());

        if (exam.getStatus() == ExamStatus.CLOSED) {
            throw new RuntimeException("CLOSED exams cannot be edited");
        }

        if (exam.getStatus() == ExamStatus.PUBLISHED) {
            if (request.getSeedSql() != null && !request.getSeedSql().equals(exam.getSeedSql())) {
                throw new RuntimeException("Cannot change seed SQL of a PUBLISHED exam");
            }
        }

        if (request.getTitle() != null) exam.setTitle(request.getTitle());
        if (request.getDescription() != null) exam.setDescription(request.getDescription());
        if (request.getVisibilityMode() != null) exam.setVisibilityMode(request.getVisibilityMode());
        if (request.getMaxAttempts() != null) exam.setMaxAttempts(request.getMaxAttempts());
        
        if (request.getTimeLimitMins() != null) exam.setTimeLimitMins(request.getTimeLimitMins());
        
        if (exam.getStatus() == ExamStatus.DRAFT) {
            if (request.getSeedSql() != null) exam.setSeedSql(request.getSeedSql());
        }

        return toResponse(examRepository.save(exam));
    }

    @Override
    public ExamResponse publishExam(String examId) {
        Exam exam = findById(examId);
        assertTeacherOwnsCourse(exam.getCourseId());

        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT exams can be published");
        }
        if (exam.getSeedSql() == null || exam.getSeedSql().isBlank()) {
            throw new RuntimeException("Cannot publish: seed_sql is missing");
        }
        if (exam.getVisibilityMode() == null) {
            throw new RuntimeException("Cannot publish: visibility_mode is missing");
        }

        exam.setStatus(ExamStatus.PUBLISHED);
        exam.setPublishedAt(Instant.now());

        return toResponse(examRepository.save(exam));
    }

    @Override
    public ExamResponse unpublishExam(String examId) {
        Exam exam = findById(examId);
        assertTeacherOwnsCourse(exam.getCourseId());

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("Only PUBLISHED exams can be unpublished");
        }

        exam.setStatus(ExamStatus.DRAFT);
        exam.setPublishedAt(null);

        return toResponse(examRepository.save(exam));
    }

    @Override
    public ExamResponse closeExam(String examId) {
        Exam exam = findById(examId);
        assertTeacherOwnsCourse(exam.getCourseId());

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("Only PUBLISHED exams can be closed");
        }

        exam.setStatus(ExamStatus.CLOSED);
        return toResponse(examRepository.save(exam));
    }

    @Override
    public void deleteExam(String examId) {
        Exam exam = findById(examId);
        assertTeacherOwnsCourse(exam.getCourseId());

        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT exams can be deleted");
        }

        examRepository.delete(exam);
    }

    private Exam findById(String examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found: " + examId));
    }

    private ExamResponse toResponse(Exam exam) {
        ExamResponse response = ExamMapper.toResponse(exam);
        
        courseRepository.findById(Long.parseLong(exam.getCourseId()))
                .ifPresent(course -> response.setCourseName(course.getName()));

        int questionCount = Math.toIntExact(questionRepository.countByExamId(java.util.UUID.fromString(exam.getId())));
        response.setQuestionCount(questionCount);
        response.setQuestionsCount(questionCount);
        if (currentUserService.hasRole(UserTypes.STUDENT)) {
            response.setSeedSql(null);
        }
        return response;
    }

    private ExamResponse toResponse(Exam exam, Integer questionCount) {
        ExamResponse response = ExamMapper.toResponse(exam);

        courseRepository.findById(Long.parseLong(exam.getCourseId()))
                .ifPresent(course -> response.setCourseName(course.getName()));

        int safeQuestionCount = questionCount != null ? questionCount : 0;
        response.setQuestionCount(safeQuestionCount);
        response.setQuestionsCount(safeQuestionCount);
        if (currentUserService.hasRole(UserTypes.STUDENT)) {
            response.setSeedSql(null);
        }
        return response;
    }

    private List<ExamResponse> toResponsesForCurrentUser(List<Exam> exams) {
        if (exams.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> questionCounts = loadQuestionCounts(exams);
        boolean studentCaller = currentUserService.hasRole(UserTypes.STUDENT);

        if (!studentCaller) {
            return exams.stream()
                    .map(exam -> toResponse(exam, questionCounts.get(exam.getId())))
                    .collect(Collectors.toList());
        }

        StudentAccessContext accessContext = resolveStudentAccessContext();
        return exams.stream()
                .filter(exam -> canCurrentUserAccessExam(exam, accessContext))
                .map(exam -> toResponse(exam, questionCounts.get(exam.getId())))
                .collect(Collectors.toList());
    }

    private Map<String, Integer> loadQuestionCounts(List<Exam> exams) {
        List<UUID> examIds = exams.stream()
                .map(exam -> UUID.fromString(exam.getId()))
                .toList();

        if (examIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Object[] row : questionRepository.countByExamIds(examIds)) {
            counts.put(row[0].toString(), ((Number) row[1]).intValue());
        }
        return counts;
    }

    private StudentAccessContext resolveStudentAccessContext() {
        Student student = studentRepository.findByUser_Id(currentUserService.requireCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        Set<String> enrolledCourseIds = courseEnrollmentRepository.findByStudentId(student.getId())
                .stream()
                .map(enrollment -> enrollment.getCourse().getId().toString())
                .collect(Collectors.toSet());

        return new StudentAccessContext(student, enrolledCourseIds);
    }

    private void assertCurrentUserCanAccessExam(Exam exam) {
        if (!canCurrentUserAccessExam(exam)) {
            throw new RuntimeException("Access denied to exam: " + exam.getId());
        }
    }

    private boolean canCurrentUserAccessExam(Exam exam) {
        if (!currentUserService.hasRole(UserTypes.STUDENT)) {
            return true;
        }

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            return false;
        }

        return canCurrentUserAccessExam(exam, resolveStudentAccessContext());
    }

    private boolean canCurrentUserAccessExam(Exam exam, StudentAccessContext accessContext) {
        if (accessContext.student().getCourse() != null
                && Objects.equals(accessContext.student().getCourse().getId().toString(), exam.getCourseId())) {
            return true;
        }

        return accessContext.enrolledCourseIds().contains(exam.getCourseId());
    }

    private void assertTeacherOwnsCourse(String courseId) {
        if (currentUserService.hasRole(UserTypes.TEACHER)) {
            com.year2.queryme.model.Course course = courseRepository.findById(Long.parseLong(courseId))
                    .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            if (!course.getTeacher().getUser().getEmail().equals(email)) {
                throw new RuntimeException("Teachers can only manage exams for their own courses");
            }
        }
    }

    private record StudentAccessContext(Student student, Set<String> enrolledCourseIds) {
    }
}
