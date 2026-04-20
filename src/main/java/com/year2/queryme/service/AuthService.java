package com.year2.queryme.service;

import com.year2.queryme.model.enums.UserTypes;
import com.year2.queryme.model.dto.InitializeSuperAdminRequest;
import com.year2.queryme.model.dto.JwtResponse;
import com.year2.queryme.model.dto.LoginRequest;
import com.year2.queryme.model.dto.MessageResponse;
import com.year2.queryme.model.dto.SignupRequest;
import com.year2.queryme.repository.UserRepository;
import com.year2.queryme.security.JwtUtils;
import com.year2.queryme.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    StudentService studentService;

    @Autowired
    AdminService adminService;

    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getName(),
                roles,
                userDetails.isMustResetPassword()));
    }

    public ResponseEntity<?> registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        UserTypes role = (signUpRequest.getRole() != null) ? signUpRequest.getRole() : UserTypes.STUDENT;
        if (role != UserTypes.STUDENT) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Public signup only supports STUDENT accounts"));
        }

        studentService.registerStudent(
            signUpRequest.getEmail(),
            signUpRequest.getPassword(),
            signUpRequest.getFullName() != null ? signUpRequest.getFullName() : signUpRequest.getName(),
            null, null,
            signUpRequest.getStudentNumber()
        );

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    public ResponseEntity<?> initializeFirstSuperAdmin(InitializeSuperAdminRequest request) {
        adminService.initializeFirstSuperAdmin(
                request.getEmail(),
                request.getPassword(),
                request.getFullName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Super admin initialized successfully!"));
    }
}
