package com.year2.queryme;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.year2.queryme.model.ClassGroup;
import com.year2.queryme.model.Course;
import com.year2.queryme.model.Student;
import com.year2.queryme.model.Teacher;
import com.year2.queryme.model.User;
import com.year2.queryme.model.enums.UserTypes;
import com.year2.queryme.repository.ClassGroupRepository;
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

import java.util.List;
import java.util.Map;

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
class StudentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

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
        classGroupRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void bulkRegisterStudentsCreatesAllStudents() throws Exception {
        Teacher teacher = createTeacher("teacher@example.com", "Teacher One");
        Course course = courseRepository.save(Course.builder()
                .name("Database Systems")
                .code("DB101")
                .teacher(teacher)
                .build());
        ClassGroup classGroup = classGroupRepository.save(ClassGroup.builder()
                .name("Section A")
                .course(course)
                .build());

        List<Map<String, Object>> payload = List.of(
                Map.of(
                        "email", "student1@example.com",
                        "password", "password123",
                        "fullName", "Student One",
                        "student_number", "STU-100",
                        "courseId", course.getId(),
                        "classGroupId", classGroup.getId()),
                Map.of(
                        "email", "student2@example.com",
                        "password", "password456",
                        "fullName", "Student Two",
                        "studentNumber", "STU-101"));

        mockMvc.perform(post("/students/register/bulk")
                .with(SecurityMockMvcRequestPostProcessors.user("teacher@example.com").roles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].user.email").value("student1@example.com"))
                .andExpect(jsonPath("$[0].studentNumber").value("STU-100"))
                .andExpect(jsonPath("$[0].course.id").value(course.getId()))
                .andExpect(jsonPath("$[0].classGroup.id").value(classGroup.getId()))
                .andExpect(jsonPath("$[1].user.email").value("student2@example.com"))
                .andExpect(jsonPath("$[1].studentNumber").value("STU-101"));

        assertThat(studentRepository.count()).isEqualTo(2);
        assertThat(userRepository.count()).isEqualTo(3);

        List<Student> students = studentRepository.findAll();
        assertThat(students)
                .extracting(Student::getFullName)
                .containsExactlyInAnyOrder("Student One", "Student Two");
        assertThat(students.stream()
                .filter(student -> "STU-100".equals(student.getStudentNumber()))
                .findFirst()
                .orElseThrow()
                .getCourse()
                .getId()).isEqualTo(course.getId());
    }

    @Test
    void bulkRegisterStudentsRollsBackWhenBatchContainsDuplicateEmail() throws Exception {
        List<Map<String, Object>> payload = List.of(
                Map.of(
                        "email", "duplicate@example.com",
                        "password", "password123",
                        "fullName", "Student One",
                        "student_number", "STU-200"),
                Map.of(
                        "email", "duplicate@example.com",
                        "password", "password456",
                        "fullName", "Student Two",
                        "student_number", "STU-201"));

        mockMvc.perform(post("/students/register/bulk")
                .with(SecurityMockMvcRequestPostProcessors.user("teacher@example.com").roles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Duplicate email found in bulk request: duplicate@example.com"));

        assertThat(studentRepository.count()).isZero();
        assertThat(userRepository.count()).isZero();
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
