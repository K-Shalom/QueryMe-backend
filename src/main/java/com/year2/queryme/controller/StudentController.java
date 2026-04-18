package com.year2.queryme.controller;

import com.year2.queryme.model.Student;
import com.year2.queryme.model.dto.StudentRegistrationRequest;
import com.year2.queryme.model.enums.UserTypes;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.service.CurrentUserService;
import com.year2.queryme.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Student register(@RequestBody StudentRegistrationRequest request) {
        return studentService.registerStudent(request);
    }

    @PostMapping("/register/bulk")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public List<Student> registerBulk(@RequestBody List<StudentRegistrationRequest> requests) {
        return studentService.registerStudents(requests);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Student update(@PathVariable Long id, @RequestBody Map<String, String> data) {
        return studentService.updateProfile(id, data);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Page<Student> getAll(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Student getById(@PathVariable Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        if (currentUserService.hasRole(UserTypes.STUDENT)
                && (student.getUser() == null
                || !student.getUser().getId().equals(currentUserService.requireCurrentUserId()))) {
            throw new RuntimeException("Students can only access their own profile");
        }

        return student;
    }
}
