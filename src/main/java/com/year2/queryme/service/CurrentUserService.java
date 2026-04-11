package com.year2.queryme.service;

import com.year2.queryme.model.User;
import com.year2.queryme.model.enums.UserTypes;
import com.year2.queryme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("No authenticated user found");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public UUID requireCurrentUserId() {
        return requireCurrentUser().getId();
    }

    public UserTypes requireCurrentRole() {
        return requireCurrentUser().getRole();
    }

    public boolean hasRole(UserTypes role) {
        return requireCurrentRole() == role;
    }
}
