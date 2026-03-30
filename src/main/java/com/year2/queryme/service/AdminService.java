package com.year2.queryme.service;

import com.year2.queryme.model.Admin;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.AdminRepository;
import com.year2.queryme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Transactional
    public Admin registerAdmin(String email, String password, String fullName) {
        // 1. Create User
        User user = User.builder()
                .email(email)
                .password(password)
                .role("ADMIN")
                .build();
        userRepository.save(user);

        // 2. Create Admin linked to User
        Admin admin = Admin.builder()
                .fullName(fullName)
                .user(user)
                .build();

        return adminRepository.save(admin);
    }

    @Transactional
    public Admin updateProfile(Long adminId, Map<String, String> data) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (data.containsKey("fullName")) {
            admin.setFullName(data.get("fullName"));
        }

        return adminRepository.save(admin);
    }
}
