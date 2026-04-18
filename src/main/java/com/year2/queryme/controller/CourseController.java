package com.year2.queryme.controller;

import com.year2.queryme.model.Course;
import com.year2.queryme.model.Teacher;
import com.year2.queryme.repository.CourseRepository;
import com.year2.queryme.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @PostMapping
    public Course create(@RequestBody Course course) {
        // Get the logged-in user's email from the security token
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Find the teacher record for this user
        Teacher teacher = teacherRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Teacher not found for email: " + email));
        
        // Link the course to this teacher
        course.setTeacher(teacher);
        
        return courseRepository.save(course);
    }

    @GetMapping
    public Page<Course> getAll(Pageable pageable) {
        return courseRepository.findAll(pageable);
    }
}
