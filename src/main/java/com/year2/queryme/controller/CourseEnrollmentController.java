package com.year2.queryme.controller;

import com.year2.queryme.model.Course;
import com.year2.queryme.model.CourseEnrollment;
import com.year2.queryme.model.Student;
import com.year2.queryme.repository.CourseEnrollmentRepository;
import com.year2.queryme.repository.CourseRepository;
import com.year2.queryme.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/course-enrollments")
public class CourseEnrollmentController {

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private com.year2.queryme.service.CurrentUserService currentUserService;

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public CourseEnrollment enrollStudent(
            @RequestParam(name = "courseId", required = false) String courseIdParam,
            @RequestParam(name = "course_id", required = false) String courseIdSnakeParam,
            @RequestParam(name = "studentId", required = false) String studentIdParam,
            @RequestParam(name = "student_id", required = false) String studentIdSnakeParam,
            @RequestBody(required = false) Map<String, String> data) {
        Long courseId = resolveRequiredId("courseId", firstNonBlank(
                courseIdParam,
                courseIdSnakeParam,
                valueFromBody(data, "courseId", "course_id")));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Teachers can only enroll into their own courses
        if (currentUserService.hasRole(com.year2.queryme.model.enums.UserTypes.TEACHER)) {
            String email = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();
            if (!course.getTeacher().getUser().getEmail().equals(email)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Teachers can only enroll students into their own courses");
            }
        }

        Long studentId = resolveRequiredId("studentId", firstNonBlank(
                studentIdParam,
                studentIdSnakeParam,
                valueFromBody(data, "studentId", "student_id")));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Avoid duplicate enrollments
        boolean alreadyEnrolled = courseEnrollmentRepository
                .findByCourseIdAndStudentId(courseId, studentId)
                .isPresent();
        if (alreadyEnrolled) {
            throw new RuntimeException("Student is already enrolled in this course");
        }

        CourseEnrollment enrollment = CourseEnrollment.builder()
                .id(java.util.UUID.randomUUID().toString())
                .course(course)
                .student(student)
                .build();

        return courseEnrollmentRepository.save(enrollment);
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Page<CourseEnrollment> getAllEnrollments(Pageable pageable) {
        if (currentUserService.hasRole(com.year2.queryme.model.enums.UserTypes.TEACHER)) {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            return courseEnrollmentRepository.findByCourseTeacherUserEmail(email, pageable);
        }
        return courseEnrollmentRepository.findAll(pageable);
    }

    @GetMapping("/course/{courseId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Page<CourseEnrollment> getEnrollmentsByCourse(@PathVariable Long courseId, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        if (currentUserService.hasRole(com.year2.queryme.model.enums.UserTypes.TEACHER)) {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            if (!course.getTeacher().getUser().getEmail().equals(email)) {
                throw new RuntimeException("Teachers can only view enrollments for their own courses");
            }
        }
        
        return courseEnrollmentRepository.findByCourseId(courseId, pageable);
    }

    @GetMapping("/student/{studentId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Page<CourseEnrollment> getEnrollmentsByStudent(@PathVariable Long studentId, Pageable pageable) {
        if (currentUserService.hasRole(com.year2.queryme.model.enums.UserTypes.TEACHER)) {
             String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
             return courseEnrollmentRepository.findByStudentIdAndCourseTeacherUserEmail(studentId, email, pageable);
        }
        return courseEnrollmentRepository.findByStudentId(studentId, pageable);
    }
    
    @DeleteMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public void unenrollStudent(
            @RequestParam(name = "courseId", required = false) String courseIdParam,
            @RequestParam(name = "course_id", required = false) String courseIdSnakeParam,
            @RequestParam(name = "studentId", required = false) String studentIdParam,
            @RequestParam(name = "student_id", required = false) String studentIdSnakeParam,
            @RequestBody(required = false) Map<String, String> data) {
        Long courseId = resolveRequiredId("courseId", firstNonBlank(
                courseIdParam,
                courseIdSnakeParam,
                valueFromBody(data, "courseId", "course_id")));
        Long studentId = resolveRequiredId("studentId", firstNonBlank(
                studentIdParam,
                studentIdSnakeParam,
                valueFromBody(data, "studentId", "student_id")));

        // Teachers can only unenroll from their own courses
        if (currentUserService.hasRole(com.year2.queryme.model.enums.UserTypes.TEACHER)) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            String email = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();
            if (!course.getTeacher().getUser().getEmail().equals(email)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Teachers can only unenroll students from their own courses");
            }
        }

        CourseEnrollment enrollment = courseEnrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        courseEnrollmentRepository.delete(enrollment);
    }

    private Long resolveRequiredId(String fieldName, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid number");
        }
    }

    private String valueFromBody(Map<String, String> data, String camelCaseKey, String snakeCaseKey) {
        if (data == null) {
            return null;
        }

        return firstNonBlank(data.get(camelCaseKey), data.get(snakeCaseKey));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
