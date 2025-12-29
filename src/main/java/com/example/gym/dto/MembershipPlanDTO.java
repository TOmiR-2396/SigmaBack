package com.example.gym.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlanDTO {
    public Long id;
    public String name;
    public Integer durationMonths;
    public Double price;
    public Integer daysPerWeek;
}
