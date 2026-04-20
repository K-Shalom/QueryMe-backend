package com.year2.queryme.service;

import com.year2.queryme.model.Guest;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.GuestRepository;
import com.year2.queryme.repository.UserRepository;
import com.year2.queryme.model.enums.UserTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class GuestService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailService emailService;

    @Transactional
    public Guest registerGuest(String email, String password, String fullName) {
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
                .role(UserTypes.GUEST)
                .name(fullName)
                .mustResetPassword(mustReset)
                .build();
        userRepository.save(user);
        passwordService.recordPassword(user, passwordHash);

        // 2. Create Guest linked to User
        Guest guest = Guest.builder()
                .fullName(fullName)
                .user(user)
                .build();

        return guestRepository.save(guest);
    }

    @Transactional
    public Guest updateProfile(Long guestId, Map<String, String> data) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("Guest not found"));

        if (data.containsKey("fullName")) {
            guest.setFullName(data.get("fullName"));
            User user = guest.getUser();
            if (user != null) {
                user.setName(data.get("fullName"));
                userRepository.save(user);
            }
        }
        if (data.containsKey("password")) {
            User user = guest.getUser();
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

        return guestRepository.save(guest);
    }
}
