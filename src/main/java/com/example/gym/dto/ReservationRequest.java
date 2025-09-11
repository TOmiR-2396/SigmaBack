package com.example.gym.dto;

public class ReservationRequest {
    private Long scheduleId;
    private String date; // "2025-01-15"

    // Constructor por defecto
    public ReservationRequest() {}

    // Constructor con parámetros
    public ReservationRequest(Long scheduleId, String date) {
        this.scheduleId = scheduleId;
        this.date = date;
    }

    // Getters y Setters
    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
