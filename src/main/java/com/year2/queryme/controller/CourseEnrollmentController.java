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
@RequestMapping("/api/course-enrollments")
public class CourseEnrollmentController {

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @PostMapping
    public CourseEnrollment enrollStudent(@RequestBody Map<String, String> data) {
        Long courseId = Long.parseLong(data.get("course_id"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        Long studentId = Long.parseLong(data.get("student_id"));
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
    public void unenrollStudent(@RequestBody Map<String, String> data) {
        Long courseId = Long.parseLong(data.get("course_id"));
        Long studentId = Long.parseLong(data.get("student_id"));
        
        CourseEnrollment enrollment = courseEnrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        courseEnrollmentRepository.delete(enrollment);
    }
}
