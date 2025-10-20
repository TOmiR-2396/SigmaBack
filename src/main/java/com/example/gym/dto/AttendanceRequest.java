package com.example.gym.dto;

public class AttendanceRequest {
    private Boolean attended;

    // Constructor por defecto
    public AttendanceRequest() {}

    // Constructor con parámetros
    public AttendanceRequest(Boolean attended) {
        this.attended = attended;
    }

    // Getters y Setters
    public Boolean getAttended() {
        return attended;
    }

    public void setAttended(Boolean attended) {
        this.attended = attended;
    }
}
