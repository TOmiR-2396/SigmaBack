package com.example.gym.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipInfoDTO {
    private boolean active;
    private String membershipPlan;
    private LocalDate startDate;
    private LocalDate endDate;
    private int daysRemaining;
    private int totalPayments;
    private String userStatus;
    private String membershipStatus;
    
    // Campos adicionales para compatibilidad
    private Long planId;
    private String planName;
    private Integer durationMonths;
    private Double price;
    private Integer daysPerWeek;
    private String status;
    private long daysLeft;
}
