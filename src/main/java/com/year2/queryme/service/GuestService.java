package com.year2.queryme.service;

import com.year2.queryme.model.Guest;
import com.year2.queryme.model.User;
import com.year2.queryme.repository.GuestRepository;
import com.year2.queryme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class GuestService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Transactional
    public Guest registerGuest(String email, String password, String fullName) {
        // 1. Create User
        User user = User.builder()
                .email(email)
                .password(password)
                .role("GUEST")
                .build();
        userRepository.save(user);

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
        }

        return guestRepository.save(guest);
    }
}
