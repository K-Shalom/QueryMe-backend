package com.year2.queryme.service;

import com.year2.queryme.model.ClassGroup;
import com.year2.queryme.model.Course;
import com.year2.queryme.model.Student;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.ClassGroupRepository;
import com.year2.queryme.repository.CourseRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class StudentService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Student registerStudent(String email, String password, String fullName,
                                   Long courseId, Long classGroupId) {
        // 1. Create User with BCrypt-encoded password
        User user = User.builder()
                .email(email)
                .username(email)
                .password(passwordEncoder.encode(password))
                .role("STUDENT")
                .build();
        userRepository.save(user);

        // 2. Get Course (required)
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));

        // 3. Create Student linked to User, Course and optional ClassGroup
        Student student = Student.builder()
                .fullName(fullName)
                .user(user)
                .course(course)
                .classGroup(classGroupId != null
                        ? classGroupRepository.findById(classGroupId).orElse(null)
                        : null)
                .build();

        return studentRepository.save(student);
    }

    @Transactional
    public Student updateProfile(Long studentId, Map<String, String> data) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        if (data.containsKey("fullName")) {
            student.setFullName(data.get("fullName"));
        }
        if (data.containsKey("courseId")) {
            Course course = courseRepository.findById(Long.parseLong(data.get("courseId")))
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            student.setCourse(course);
        }
        if (data.containsKey("classGroupId")) {
            ClassGroup group = classGroupRepository.findById(Long.parseLong(data.get("classGroupId")))
                    .orElseThrow(() -> new RuntimeException("Class Group not found"));
            student.setClassGroup(group);
        }

        return studentRepository.save(student);
    }
}
