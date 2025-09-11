package com.example.gym.dto;

public class ScheduleDTO {
    public Long id;
    public Integer dayOfWeek; // 0=Domingo, 1=Lunes, etc.
    public String startTime; // "08:00"
    public String endTime; // "09:00"
    public Integer maxCapacity; // 10
    public Boolean isActive; // true
    public Boolean repeatWeekly; // true
    public Integer currentReservations; // Para mostrar ocupación
    public String description;

    // Constructor por defecto
    public ScheduleDTO() {}

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getRepeatWeekly() {
        return repeatWeekly;
    }

    public void setRepeatWeekly(Boolean repeatWeekly) {
        this.repeatWeekly = repeatWeekly;
    }

    public Integer getCurrentReservations() {
        return currentReservations;
    }

    public void setCurrentReservations(Integer currentReservations) {
        this.currentReservations = currentReservations;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
