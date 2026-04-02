package com.year2.queryme.model;

import com.year2.queryme.config.StringLongConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "guests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Convert(converter = StringLongConverter.class)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
}
