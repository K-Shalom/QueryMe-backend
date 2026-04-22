package com.year2.queryme;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.year2.queryme.model.Course;
import com.year2.queryme.model.Student;
import com.year2.queryme.model.Teacher;
import com.year2.queryme.model.User;
import com.year2.queryme.model.enums.UserTypes;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

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
public class CourseEnrollmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        courseEnrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void enrollStudentAcceptsQueryParameters() throws Exception {
        Course course = courseRepository.save(Course.builder()
                .name("Database Systems")
                .code("DB101")
                .teacher(createTeacher("teacher@example.com", "Teacher One"))
                .build());

        Student student = studentRepository.save(Student.builder()
                .fullName("Student One")
                .firstName("Student")
                .lastName("One")
                .registeredAt(Instant.now())
                .studentNumber("STU-001")
                .user(buildUser("student@example.com", "Student One", UserTypes.STUDENT))
                .build());

        mockMvc.perform(post("/course-enrollments")
                .with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN"))
                .param("courseId", course.getId().toString())
                .param("studentId", student.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.id").value(course.getId()))
                .andExpect(jsonPath("$.student.id").value(student.getId()));
    }

    @Test
    void enrollStudentAcceptsCamelCaseBody() throws Exception {
        Course course = courseRepository.save(Course.builder()
                .name("Advanced SQL")
                .code("DB201")
                .teacher(createTeacher("teacher2@example.com", "Teacher Two"))
                .build());

        Student student = studentRepository.save(Student.builder()
                .fullName("Student Two")
                .firstName("Student")
                .lastName("Two")
                .registeredAt(Instant.now())
                .studentNumber("STU-002")
                .user(buildUser("student2@example.com", "Student Two", UserTypes.STUDENT))
                .build());

        mockMvc.perform(post("/course-enrollments")
                .with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "courseId", course.getId().toString(),
                        "studentId", student.getId().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.id").value(course.getId()))
                .andExpect(jsonPath("$.student.id").value(student.getId()));
    }

    private Teacher createTeacher(String email, String fullName) {
        return teacherRepository.save(Teacher.builder()
                .fullName(fullName)
                .department("Databases")
                .user(buildUser(email, fullName, UserTypes.TEACHER))
                .build());
    }

    private User buildUser(String email, String name, UserTypes role) {
        return User.builder()
                .email(email)
                .name(name)
                .passwordHash("hashed")
                .role(role)
                .build();
    }
}
