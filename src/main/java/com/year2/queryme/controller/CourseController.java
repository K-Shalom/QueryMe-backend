package com.year2.queryme.controller;

import com.year2.queryme.model.Course;
import com.year2.queryme.model.Teacher;
import com.year2.queryme.repository.CourseRepository;
import com.year2.queryme.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
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
    public List<Course> getAll() {
        List<Course> courses = courseRepository.findAll();
        System.out.println("--- DIAGNOSTIC: CURRENT COURSES IN DATABASE ---");
        courses.forEach(c -> System.out.println("ID: [" + c.getId() + "] Name: " + c.getName()));
        System.out.println("------------------------------------------------");
        return courses;
    }
}
