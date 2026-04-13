package com.year2.queryme.service;

import com.year2.queryme.model.ClassGroup;
import com.year2.queryme.model.Course;
import com.year2.queryme.model.Student;
import com.year2.queryme.model.User;
import com.year2.queryme.model.dto.StudentRegistrationRequest;
import com.year2.queryme.repository.ClassGroupRepository;
import com.year2.queryme.repository.CourseRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.repository.UserRepository;

import java.time.LocalDateTime;
import com.year2.queryme.model.enums.UserTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudentService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CurrentUserService currentUserService;

    @Transactional
    public Student registerStudent(String email, String password, String fullName,
                                   Long courseId, Long classGroupId, String studentNumber) {
        StudentRegistrationRequest request = new StudentRegistrationRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setFullName(fullName);
        request.setCourseId(courseId);
        request.setClassGroupId(classGroupId);
        request.setStudentNumber(studentNumber);
        return registerStudent(request);
    }

    @Transactional
    public Student registerStudent(StudentRegistrationRequest request) {
        StudentRegistrationRequest normalizedRequest = normalizeAndValidate(request, new HashSet<>(), new HashSet<>());
        return createStudent(normalizedRequest);
    }

    @Transactional
    public List<Student> registerStudents(List<StudentRegistrationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one student registration is required");
        }

        Set<String> seenEmails = new HashSet<>();
        Set<String> seenStudentNumbers = new HashSet<>();

        return requests.stream()
                .map(request -> createStudent(normalizeAndValidate(request, seenEmails, seenStudentNumbers)))
                .toList();
    }

    @Transactional
    public Student updateProfile(Long studentId, Map<String, String> data) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        boolean isStudentCaller = currentUserService.hasRole(UserTypes.STUDENT);
        if (isStudentCaller && (student.getUser() == null
                || !student.getUser().getId().equals(currentUserService.requireCurrentUserId()))) {
            throw new RuntimeException("Students can only update their own profile");
        }

        if (isStudentCaller && (data.containsKey("courseId") || data.containsKey("classGroupId") || data.containsKey("student_number"))) {
            throw new RuntimeException("Students cannot modify course, class group, or student number");
        }

        if (data.containsKey("fullName")) {
            student.setFullName(data.get("fullName"));
            User user = student.getUser();
            if (user != null) {
                user.setName(data.get("fullName"));
                userRepository.save(user);
            }
        }
        if (data.containsKey("student_number")) {
            student.setStudentNumber(data.get("student_number"));
        }
        if (data.containsKey("password")) {
            User user = student.getUser();
            if (user != null) {
                user.setPasswordHash(passwordEncoder.encode(data.get("password")));
                userRepository.save(user);
            }
        }
        if (data.containsKey("courseId")) {
            Course course = courseRepository.findById(Long.parseLong(data.get("courseId")))
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            student.setCourse(course);
        }
        if (data.containsKey("classGroupId")) {
            ClassGroup group = classGroupRepository.findById(Long.parseLong(data.get("classGroupId")))
                    .orElseThrow(() -> new RuntimeException("Class Group not found"));
            student.setClassGroup(group);
        }

        return studentRepository.save(student);
    }

    private StudentRegistrationRequest normalizeAndValidate(
            StudentRegistrationRequest request,
            Set<String> seenEmails,
            Set<String> seenStudentNumbers) {
        if (request == null) {
            throw new IllegalArgumentException("Student registration payload is required");
        }

        String email = trimToNull(request.getEmail());
        String fullName = trimToNull(request.getFullName());
        String studentNumber = trimToNull(request.getStudentNumber());

        if (email == null) {
            throw new IllegalArgumentException("Student email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Student password is required");
        }
        if (fullName == null) {
            throw new IllegalArgumentException("Student fullName is required");
        }
        if (!seenEmails.add(email)) {
            throw new IllegalArgumentException("Duplicate email found in bulk request: " + email);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use: " + email);
        }
        if (studentNumber != null) {
            if (!seenStudentNumbers.add(studentNumber)) {
                throw new IllegalArgumentException("Duplicate student number found in bulk request: " + studentNumber);
            }
            if (studentRepository.existsByStudentNumber(studentNumber)) {
                throw new IllegalArgumentException("Student number is already in use: " + studentNumber);
            }
        }

        StudentRegistrationRequest normalizedRequest = new StudentRegistrationRequest();
        normalizedRequest.setEmail(email);
        normalizedRequest.setPassword(request.getPassword());
        normalizedRequest.setFullName(fullName);
        normalizedRequest.setCourseId(request.getCourseId());
        normalizedRequest.setClassGroupId(request.getClassGroupId());
        normalizedRequest.setStudentNumber(studentNumber);
        return normalizedRequest;
    }

    private Student createStudent(StudentRegistrationRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserTypes.STUDENT)
                .name(request.getFullName())
                .build();
        userRepository.save(user);

        Course course = request.getCourseId() != null
                ? courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + request.getCourseId()))
                : null;

        ClassGroup classGroup = request.getClassGroupId() != null
                ? classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new RuntimeException("Class Group not found with id: " + request.getClassGroupId()))
                : null;

        String[] nameParts = request.getFullName().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        Student student = Student.builder()
                .fullName(request.getFullName())
                .firstName(firstName)
                .lastName(lastName)
                .registeredAt(LocalDateTime.now())
                .studentNumber(request.getStudentNumber())
                .user(user)
                .course(course)
                .classGroup(classGroup)
                .build();

        return studentRepository.save(student);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
