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
import org.springframework.security.core.context.SecurityContextHolder;
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
    @PreAuthorize("hasRole('ADMIN')")
    public Student register(@RequestBody StudentRegistrationRequest request) {
        return studentService.registerStudent(request);
    }

    @PostMapping("/register/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Student> registerBulk(@RequestBody List<StudentRegistrationRequest> requests) {
        return studentService.registerStudents(requests);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Student update(@PathVariable String id, @RequestBody Map<String, String> data) {
        return studentService.updateProfile(id, data);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Page<Student> getAll(Pageable pageable) {
        if (currentUserService.hasRole(UserTypes.TEACHER)) {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return studentRepository.findAllByTeacherEmail(email, pageable);
        }
        return studentRepository.findAll(pageable);
    }

    /**
     * Returns ALL registered students regardless of course assignment (for enrollment pickers).
     * Filtered to only return users with the STUDENT role.
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Page<Student> getAllStudents(Pageable pageable) {
        return studentRepository.findAllStudents(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Student getById(@PathVariable String id) {
        Student student = studentService.findStudentByIdOrUserId(id);

        if (currentUserService.hasRole(UserTypes.STUDENT)
                && (student.getUser() == null
                || !student.getUser().getId().equals(currentUserService.requireCurrentUserId()))) {
            throw new RuntimeException("Students can only access their own profile");
        }

        return student;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public org.springframework.http.ResponseEntity<?> delete(@PathVariable String id) {
        studentService.deleteStudent(id);
        return org.springframework.http.ResponseEntity.ok(new com.year2.queryme.model.dto.MessageResponse("Student deleted successfully"));
    }
}
