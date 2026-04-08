package com.year2.queryme.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Table(
    name = "course_enrollments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEnrollment {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Student student;

    @Column(name = "enrolled_at")
    @Builder.Default
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @PrePersist
    protected void onEnroll() {
        if (enrolledAt == null) {
            enrolledAt = LocalDateTime.now();
        }
    }
}
