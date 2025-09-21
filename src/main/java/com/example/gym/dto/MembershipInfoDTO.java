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
    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setDaysPerWeek(Integer daysPerWeek) {
        this.daysPerWeek = daysPerWeek;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDaysLeft(long daysLeft) {
        this.daysLeft = daysLeft;
    }
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
    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getPlanId() {
        return this.planId;
    }
    private String planName;
    private Integer durationMonths;
    private Double price;
    private Integer daysPerWeek;
    private String status;
    private long daysLeft;
}
