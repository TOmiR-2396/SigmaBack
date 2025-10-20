package com.example.gym.model;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "reservations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_res_unique_user_schedule_date_status",
            columnNames = {"user_id", "schedule_id", "date", "status"}
        )
    },
    indexes = {
        @Index(name = "idx_res_schedule_date_status", columnList = "schedule_id,date,status"),
        @Index(name = "idx_res_user_schedule_date_status", columnList = "user_id,schedule_id,date,status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    // Campos de presentismo/asistencia
    @Column(nullable = false)
    @Builder.Default
    private Boolean attended = false;

    @Column
    private LocalDateTime attendedAt;

    public enum ReservationStatus {
        CONFIRMED, CANCELLED
    }
}
