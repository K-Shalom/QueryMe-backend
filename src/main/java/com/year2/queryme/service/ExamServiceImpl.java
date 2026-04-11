package com.year2.queryme.service;

import com.year2.queryme.model.Exam;
import com.year2.queryme.model.Student;
import com.year2.queryme.model.dto.*;
import com.year2.queryme.model.enums.ExamStatus;
import com.year2.queryme.model.enums.UserTypes;
import com.year2.queryme.model.mapper.ExamMapper;
import com.year2.queryme.repository.CourseEnrollmentRepository;
import com.year2.queryme.repository.ExamRepository;
import com.year2.queryme.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final CurrentUserService currentUserService;
    private final StudentRepository studentRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    @Override
    public ExamResponse createExam(CreateExamRequest request) {
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
        List<Exam> exams = currentUserService.hasRole(UserTypes.STUDENT)
                ? examRepository.findByCourseIdAndStatus(courseId, ExamStatus.PUBLISHED)
                : examRepository.findByCourseId(courseId);

        return exams.stream()
                .filter(this::canCurrentUserAccessExam)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExamResponse> getPublishedExams() {
        return examRepository.findByStatus(ExamStatus.PUBLISHED)
                .stream()
                .filter(this::canCurrentUserAccessExam)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ExamResponse updateExam(String examId, UpdateExamRequest request) {
        Exam exam = findById(examId);

        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT exams can be edited");
        }

        if (request.getTitle() != null) exam.setTitle(request.getTitle());
        if (request.getDescription() != null) exam.setDescription(request.getDescription());
        if (request.getVisibilityMode() != null) exam.setVisibilityMode(request.getVisibilityMode());
        if (request.getTimeLimitMins() != null) exam.setTimeLimitMins(request.getTimeLimitMins());
        if (request.getMaxAttempts() != null) exam.setMaxAttempts(request.getMaxAttempts());
        if (request.getSeedSql() != null) exam.setSeedSql(request.getSeedSql());

        return toResponse(examRepository.save(exam));
    }

    @Override
    public ExamResponse publishExam(String examId) {
        Exam exam = findById(examId);

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
        exam.setPublishedAt(LocalDateTime.now());

        return toResponse(examRepository.save(exam));
    }

    @Override
    public ExamResponse unpublishExam(String examId) {
        Exam exam = findById(examId);

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

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("Only PUBLISHED exams can be closed");
        }

        exam.setStatus(ExamStatus.CLOSED);
        return toResponse(examRepository.save(exam));
    }

    @Override
    public void deleteExam(String examId) {
        Exam exam = findById(examId);

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
        if (currentUserService.hasRole(UserTypes.STUDENT)) {
            response.setSeedSql(null);
        }
        return response;
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

        Student student = studentRepository.findByUser_Id(currentUserService.requireCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        if (student.getCourse() != null && Objects.equals(student.getCourse().getId().toString(), exam.getCourseId())) {
            return true;
        }

        Set<String> enrolledCourseIds = courseEnrollmentRepository.findByStudentId(student.getId())
                .stream()
                .map(enrollment -> enrollment.getCourse().getId().toString())
                .collect(Collectors.toSet());

        return enrolledCourseIds.contains(exam.getCourseId());
    }
}
