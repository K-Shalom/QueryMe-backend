package com.year2.queryme.service;

import com.year2.queryme.model.Teacher;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.TeacherRepository;
import com.year2.queryme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class TeacherService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Transactional
    public Teacher registerTeacher(String email, String password, String fullName) {
        // 1. Create User
        User user = User.builder()
                .email(email)
                .password(password)
                .role("TEACHER")
                .build();
        userRepository.save(user);

        // 2. Create Teacher linked to User
        Teacher teacher = Teacher.builder()
                .fullName(fullName)
                .user(user)
                .build();

        return teacherRepository.save(teacher);
    }

    @Transactional
    public Teacher updateProfile(Long teacherId, Map<String, String> data) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (data.containsKey("fullName")) {
            teacher.setFullName(data.get("fullName"));
        }

        return teacherRepository.save(teacher);
    }
}
