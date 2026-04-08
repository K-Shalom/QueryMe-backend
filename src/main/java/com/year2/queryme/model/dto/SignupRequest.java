package com.year2.queryme.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import com.year2.queryme.model.enums.UserTypes;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class SignupRequest {
    @NotBlank
    @Email
    private String email;

    private String name;

    private UserTypes role; // Strictly enforce enum usage and single role system

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

    @NotBlank
    @Size(min = 3, max = 50)
    private String fullName;

    @JsonProperty("student_number")
    private String studentNumber;

    private String department;
}
