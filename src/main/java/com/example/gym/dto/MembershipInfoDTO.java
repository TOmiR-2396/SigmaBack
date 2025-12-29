package com.example.gym.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipInfoDTO {
    public Long planId;
    public String planName;
    public Integer durationMonths;
    public Double price;
    public Integer daysPerWeek;
    public LocalDate startDate;
    public LocalDate endDate;
    public String status;
    public long daysLeft;
}
