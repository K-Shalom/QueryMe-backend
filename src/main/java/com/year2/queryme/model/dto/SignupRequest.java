package com.year2.queryme.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class SignupRequest {
    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    private Set<String> roles; // Maps to single role in DB, but keeps UI flexible

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;
}
