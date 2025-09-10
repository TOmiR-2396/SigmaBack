package com.example.gym.dto;

import java.time.LocalDate;

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
