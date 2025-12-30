package com.example.gym.model;

import com.example.gym.tenant.TenantEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.Filter;

// Import Member class if it exists in the same package or another package
import com.example.gym.model.User;

@Entity
@Table(name = "attendances")
@Filter(name = "tenantFilter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime checkInTime = LocalDateTime.now();
}
