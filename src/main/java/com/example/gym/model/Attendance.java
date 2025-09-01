package com.example.gym.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

// Import Member class if it exists in the same package or another package
import com.example.gym.model.User;

@Entity
@Table(name = "attendances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime checkInTime = LocalDateTime.now();
}
