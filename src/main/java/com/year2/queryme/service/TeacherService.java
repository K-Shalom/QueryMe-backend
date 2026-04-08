package com.year2.queryme.service;

import com.year2.queryme.model.Teacher;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.TeacherRepository;
import com.year2.queryme.repository.UserRepository;
import com.year2.queryme.model.enums.UserTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class TeacherService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Teacher registerTeacher(String email, String password, String fullName, String department) {
        // 1. Create User with BCrypt-encoded password
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(UserTypes.TEACHER)
                .name(fullName)
                .build();
        userRepository.save(user);

        // 2. Create Teacher linked to User
        Teacher teacher = Teacher.builder()
                .fullName(fullName)
                .department(department)
                .user(user)
                .build();

        return teacherRepository.save(teacher);
    }

    @Transactional
    public Teacher updateProfile(Long teacherId, Map<String, String> data) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + teacherId));

        if (data.containsKey("fullName")) {
            teacher.setFullName(data.get("fullName"));
            User user = teacher.getUser();
            if (user != null) {
                user.setName(data.get("fullName"));
                userRepository.save(user);
            }
        }
        if (data.containsKey("department")) {
            teacher.setDepartment(data.get("department"));
        }
        if (data.containsKey("password")) {
            User user = teacher.getUser();
            if (user != null) {
                user.setPasswordHash(passwordEncoder.encode(data.get("password")));
                userRepository.save(user);
            }
        }

        return teacherRepository.save(teacher);
    }
}
