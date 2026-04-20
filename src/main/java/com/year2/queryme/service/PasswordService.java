package com.year2.queryme.service;

import com.year2.queryme.model.PasswordHistory;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.PasswordHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    private static final SecureRandom random = new SecureRandom();

    public String generateTemporaryPassword() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean isPasswordUsed(User user, String newPassword) {
        if (user.getPasswordHistories() == null) return false;
        
        return user.getPasswordHistories().stream()
                .anyMatch(history -> passwordEncoder.matches(newPassword, history.getPasswordHash()));
    }

    public void recordPassword(User user, String passwordHash) {
        PasswordHistory history = PasswordHistory.builder()
                .user(user)
                .passwordHash(passwordHash)
                .build();
        passwordHistoryRepository.save(history);
    }
}
