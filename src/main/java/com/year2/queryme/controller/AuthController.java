package com.year2.queryme.controller;

import com.year2.queryme.model.dto.InitializeSuperAdminRequest;
import com.year2.queryme.model.dto.LoginRequest;
import com.year2.queryme.model.dto.SignupRequest;
import com.year2.queryme.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @Autowired
    com.year2.queryme.repository.AdminRepository adminRepository;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticateUser(loginRequest);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        return authService.registerUser(signUpRequest);
    }

    @PostMapping("/bootstrap/super-admin")
    public ResponseEntity<?> initializeFirstSuperAdmin(
            @Valid @RequestBody InitializeSuperAdminRequest request) {
        return authService.initializeFirstSuperAdmin(request);
    }

    @PostMapping("/bootstrap/reset")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> resetBootstrap() {
        adminRepository.deleteAll();
        return ResponseEntity.ok(new com.year2.queryme.model.dto.MessageResponse("Bootstrap status reset. You can now bootstrap again."));
    }
}
