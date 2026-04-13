package com.year2.queryme.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentRegistrationRequest {

    private String email;
    private String password;
    private String fullName;
    private Long courseId;
    private Long classGroupId;

    @JsonAlias({"student_number", "studentNumber"})
    private String studentNumber;
}
