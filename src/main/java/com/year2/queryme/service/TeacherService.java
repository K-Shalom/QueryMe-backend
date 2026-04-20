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

@Service
public class TeacherService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailService emailService;

    @Transactional
    public Teacher registerTeacher(String email, String password, String fullName, String department) {
        boolean mustReset = false;
        if (password == null || password.isBlank()) {
            password = passwordService.generateTemporaryPassword();
            mustReset = true;
            emailService.sendTemporaryPassword(email, password);
        }

        String passwordHash = passwordEncoder.encode(password);
        User user = User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .role(UserTypes.TEACHER)
                .name(fullName)
                .mustResetPassword(mustReset)
                .build();
        userRepository.save(user);
        passwordService.recordPassword(user, passwordHash);

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

        if (currentUserService.hasRole(UserTypes.TEACHER)
                && (teacher.getUser() == null
                || !teacher.getUser().getId().equals(currentUserService.requireCurrentUserId()))) {
            throw new RuntimeException("Teachers can only update their own profile");
        }

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
                String newPassword = data.get("password");
                if (passwordService.isPasswordUsed(user, newPassword)) {
                    throw new RuntimeException("Cannot reuse a previous password");
                }
                String hash = passwordEncoder.encode(newPassword);
                user.setPasswordHash(hash);
                user.setMustResetPassword(false);
                userRepository.save(user);
                passwordService.recordPassword(user, hash);
            }
        }

        return teacherRepository.save(teacher);
    }
}
