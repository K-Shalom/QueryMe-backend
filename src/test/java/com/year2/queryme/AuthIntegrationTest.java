package com.year2.queryme;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.year2.queryme.model.dto.InitializeSuperAdminRequest;
import com.year2.queryme.model.dto.LoginRequest;
import com.year2.queryme.model.dto.SignupRequest;
import com.year2.queryme.repository.AdminRepository;
import com.year2.queryme.repository.CourseEnrollmentRepository;
import com.year2.queryme.repository.CourseRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.repository.TeacherRepository;
import com.year2.queryme.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        courseEnrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        teacherRepository.deleteAll();
        adminRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testSignupAndSignin() throws Exception {
        // 1. Signup
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("test@example.com");
        signupRequest.setName("Test User");
        signupRequest.setFullName("Test User");
        signupRequest.setPassword("password123");
        signupRequest.setRole(com.year2.queryme.model.enums.UserTypes.STUDENT);

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        // 2. Signin
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    public void testInitializeFirstSuperAdminOnlyOnce() throws Exception {
        InitializeSuperAdminRequest request = new InitializeSuperAdminRequest();
        request.setEmail("root@example.com");
        request.setPassword("password123");
        request.setFullName("Root Admin");

        mockMvc.perform(post("/auth/bootstrap/super-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Super admin initialized successfully!"));

        assertThat(adminRepository.findAll())
                .singleElement()
                .satisfies(admin -> {
                    assertThat(admin.getFullName()).isEqualTo("Root Admin");
                    assertThat(admin.getSuperAdmin()).isTrue();
                    assertThat(admin.getUser()).isNotNull();
                    assertThat(admin.getUser().getRole())
                            .isEqualTo(com.year2.queryme.model.enums.UserTypes.ADMIN);
                });

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("root@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("root@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));

        InitializeSuperAdminRequest secondRequest = new InitializeSuperAdminRequest();
        secondRequest.setEmail("another@example.com");
        secondRequest.setPassword("password123");
        secondRequest.setFullName("Another Admin");

        mockMvc.perform(post("/auth/bootstrap/super-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A super admin already exists"));
    }
}
