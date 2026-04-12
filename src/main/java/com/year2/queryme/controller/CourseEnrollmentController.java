package com.year2.queryme.controller;

import com.year2.queryme.model.Course;
import com.year2.queryme.model.CourseEnrollment;
import com.year2.queryme.model.Student;
import com.year2.queryme.repository.CourseEnrollmentRepository;
import com.year2.queryme.repository.CourseRepository;
import com.year2.queryme.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @PostMapping
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

        Long studentId = resolveRequiredId("studentId", firstNonBlank(
                studentIdParam,
                studentIdSnakeParam,
                valueFromBody(data, "studentId", "student_id")));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        CourseEnrollment enrollment = CourseEnrollment.builder()
                .id(UUID.randomUUID().toString())
                .course(course)
                .student(student)
                .build();

        return courseEnrollmentRepository.save(enrollment);
    }

    @GetMapping
    public List<CourseEnrollment> getAllEnrollments() {
        return courseEnrollmentRepository.findAll();
    }

    @GetMapping("/course/{courseId}")
    public List<CourseEnrollment> getEnrollmentsByCourse(@PathVariable Long courseId) {
        return courseEnrollmentRepository.findByCourseId(courseId);
    }

    @GetMapping("/student/{studentId}")
    public List<CourseEnrollment> getEnrollmentsByStudent(@PathVariable Long studentId) {
        return courseEnrollmentRepository.findByStudentId(studentId);
    }
    
    @DeleteMapping
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
