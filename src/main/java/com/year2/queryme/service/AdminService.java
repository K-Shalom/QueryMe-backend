package com.year2.queryme.service;

import com.year2.queryme.model.Admin;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.AdminRepository;
import com.year2.queryme.repository.UserRepository;
import com.year2.queryme.model.enums.UserTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailService emailService;

    @Transactional
    public Admin registerAdmin(String email, String password, String fullName) {
        return createAdmin(email, password, fullName, false);
    }

    @Transactional
    public Admin initializeFirstSuperAdmin(String email, String password, String fullName) {
        if (adminRepository.existsBySuperAdminTrue()) {
            throw new IllegalStateException("A super admin already exists");
        }

        return createAdmin(email, password, fullName, true);
    }

    @Transactional
    public Admin updateProfile(Long adminId, Map<String, String> data) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (currentUserService.hasRole(UserTypes.ADMIN)
                && (admin.getUser() == null
                || !admin.getUser().getId().equals(currentUserService.requireCurrentUserId()))) {
            throw new RuntimeException("Admins can only update their own profile");
        }

        if (data.containsKey("fullName")) {
            admin.setFullName(data.get("fullName"));
            User user = admin.getUser();
            if (user != null) {
                user.setName(data.get("fullName"));
                userRepository.save(user);
            }
        }
        if (data.containsKey("password")) {
            User user = admin.getUser();
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

        return adminRepository.save(admin);
    }

    private Admin createAdmin(String email, String password, String fullName, boolean superAdmin) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

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
                .role(UserTypes.ADMIN)
                .name(fullName)
                .mustResetPassword(mustReset)
                .build();
        userRepository.save(user);
        passwordService.recordPassword(user, passwordHash);

        Admin admin = Admin.builder()
                .fullName(fullName)
                .superAdmin(superAdmin)
                .user(user)
                .build();

        return adminRepository.save(admin);
    }
}
