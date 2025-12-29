package com.example.gym.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDTO {
    // Membresías
    private Long totalActiveMembers;
    private Long totalExpiredMembers;
    private Long totalCancelledMembers;
    private Double totalMonthlyRevenue;
    private Map<String, Long> membersByPlan;
    
    // Asistencia
    private Long totalAttendanceThisMonth;
    private Double attendanceRate;
    private List<AttendanceByDay> attendanceByDay;
    
    // Ejercicios
    private Long totalExercises;
    private Long totalExerciseComments;
    private List<PopularExercise> popularExercises;
    
    // Trainers
    private Long totalTrainers;
    private List<TrainerStats> trainerStats;
    
    // Usuarios
    private Long totalUsers;
    private Long totalMembers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceByDay {
        private String day;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PopularExercise {
        private Long exerciseId;
        private String name;
        private Long commentCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrainerStats {
        private Long trainerId;
        private String trainerName;
        private Long membersAssigned;
        private Long commentsGiven;
    }
}
