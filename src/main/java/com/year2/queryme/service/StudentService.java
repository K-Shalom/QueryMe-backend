package com.year2.queryme.service;

import com.year2.queryme.model.ClassGroup;
import com.year2.queryme.model.Course;
import com.year2.queryme.model.Student;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.ClassGroupRepository;
import com.year2.queryme.repository.CourseRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.repository.UserRepository;

import java.time.LocalDateTime;
import com.year2.queryme.model.enums.UserTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

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
                                   Long courseId, Long classGroupId, String studentNumber) {
        // 1. Create User with BCrypt-encoded password
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(UserTypes.STUDENT)
                .name(fullName)
                .build();
        userRepository.save(user);

        // 2. Get Course (optional at registration if from auth service)
        Course course = (courseId != null) ? courseRepository.findById(courseId).orElse(null) : null;

        // Split full name for shared DB legacy columns
        String[] nameParts = fullName != null ? fullName.split(" ", 2) : new String[]{"Unknown", ""};
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // 3. Create Student linked to User, Course and optional ClassGroup
        Student student = Student.builder()
                .fullName(fullName)
                .firstName(firstName)
                .lastName(lastName)
                .registeredAt(LocalDateTime.now())
                .studentNumber(studentNumber)
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
            User user = student.getUser();
            if (user != null) {
                user.setName(data.get("fullName"));
                userRepository.save(user);
            }
        }
        if (data.containsKey("student_number")) {
            student.setStudentNumber(data.get("student_number"));
        }
        if (data.containsKey("password")) {
            User user = student.getUser();
            if (user != null) {
                user.setPasswordHash(passwordEncoder.encode(data.get("password")));
                userRepository.save(user);
            }
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
