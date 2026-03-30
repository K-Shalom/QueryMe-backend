package com.year2.queryme.controller;

import com.year2.queryme.model.Student;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @PostMapping("/register")
    public Student register(@RequestBody Map<String, String> data) {
        return studentService.registerStudent(
                data.get("email"),
                data.get("password"),
                data.get("fullName"),
                data.containsKey("courseId") ? Long.parseLong(data.get("courseId")) : null,
                data.containsKey("classGroupId") ? Long.parseLong(data.get("classGroupId")) : null
        );
    }

    @PutMapping("/{id}")
    public Student update(@PathVariable Long id, @RequestBody Map<String, String> data) {
        return studentService.updateProfile(id, data);
    }

    @GetMapping
    public List<Student> getAll() {
        return studentRepository.findAll();
    }
}
