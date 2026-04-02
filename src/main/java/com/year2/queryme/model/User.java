package com.year2.queryme.model;

import com.year2.queryme.config.StringLongConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Convert(converter = StringLongConverter.class)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(name = "password", nullable = false)
    private String passwordMirror;

    @Column(nullable = false)
    private String role; // TEACHER, STUDENT, ADMIN or GUEST

    @PrePersist
    public void prePersist() {
        if (this.username == null) {
            this.username = this.email;
        }
        // Mirror the password to the old 'password' column
        this.passwordMirror = this.password;
    }
}
