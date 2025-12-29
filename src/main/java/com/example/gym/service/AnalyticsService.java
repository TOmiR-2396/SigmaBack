package com.example.gym.service;

import com.example.gym.dto.AnalyticsDTO;
import com.example.gym.model.*;
import com.example.gym.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AnalyticsService: Métricas y analítica de datos para SigmaGym
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final ExerciseRepository exerciseRepository;

    public AnalyticsDTO getGeneralAnalytics() {
        log.debug("Getting general analytics");

        long totalActiveMembers = countActiveMembers();
        long totalExpiredMembers = countExpiredMembers();
        long totalCancelledMembers = countCancelledMembers();
        double totalMonthlyRevenue = calculateMonthlyRevenue();
        Map<String, Long> membersByPlan = getMembersByPlan();
        long totalExercises = exerciseRepository.count();
        long totalUsers = userRepository.count();
        long totalTrainers = countTrainers();

        return AnalyticsDTO.builder()
                .totalActiveMembers(totalActiveMembers)
                .totalExpiredMembers(totalExpiredMembers)
                .totalCancelledMembers(totalCancelledMembers)
                .totalMonthlyRevenue(totalMonthlyRevenue)
                .membersByPlan(membersByPlan)
                .totalAttendanceThisMonth(0L)
                .attendanceRate(0.0)
                .attendanceByDay(new java.util.ArrayList<>())
                .totalExercises(totalExercises)
                .totalUsers(totalUsers)
                .totalMembers(totalActiveMembers)
                .totalTrainers(totalTrainers)
                .build();
    }

    public AnalyticsDTO getMembershipAnalytics() {
        log.debug("Getting membership analytics");

        long totalActiveMembers = countActiveMembers();
        long totalExpiredMembers = countExpiredMembers();
        long totalCancelledMembers = countCancelledMembers();
        double totalMonthlyRevenue = calculateMonthlyRevenue();
        Map<String, Long> membersByPlan = getMembersByPlan();

        return AnalyticsDTO.builder()
                .totalActiveMembers(totalActiveMembers)
                .totalExpiredMembers(totalExpiredMembers)
                .totalCancelledMembers(totalCancelledMembers)
                .totalMonthlyRevenue(totalMonthlyRevenue)
                .membersByPlan(membersByPlan)
                .build();
    }

    public AnalyticsDTO getAttendanceAnalytics() {
        log.debug("Getting attendance analytics");

        return AnalyticsDTO.builder()
                .totalAttendanceThisMonth(0L)
                .attendanceRate(0.0)
                .attendanceByDay(new java.util.ArrayList<>())
                .build();
    }

    public AnalyticsDTO getExerciseAnalytics() {
        log.debug("Getting exercise analytics");

        long totalExercises = exerciseRepository.count();

        return AnalyticsDTO.builder()
                .totalExercises(totalExercises)
                .build();
    }

    public AnalyticsDTO getTrainerAnalytics() {
        log.debug("Getting trainer analytics");

        long totalTrainers = countTrainers();

        return AnalyticsDTO.builder()
                .totalTrainers(totalTrainers)
                .build();
    }

    // ============ HELPERS ============

    private long countActiveMembers() {
        return subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == Subscription.Status.ACTIVE)
                .count();
    }

    private long countExpiredMembers() {
        return subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == Subscription.Status.EXPIRED)
                .count();
    }

    private long countCancelledMembers() {
        return subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == Subscription.Status.CANCELED)
                .count();
    }

    private long countTrainers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.TRAINER)
                .count();
    }

    private double calculateMonthlyRevenue() {
        YearMonth currentMonth = YearMonth.now();
        return subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == Subscription.Status.ACTIVE)
                .filter(s -> {
                    YearMonth subMonth = YearMonth.from(s.getStartDate());
                    return subMonth.equals(currentMonth);
                })
                .mapToDouble(s -> s.getPlan().getPrice())
                .sum();
    }

    private Map<String, Long> getMembersByPlan() {
        return subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == Subscription.Status.ACTIVE)
                .collect(Collectors.groupingBy(
                        s -> s.getPlan().getName(),
                        Collectors.counting()
                ));
    }

    private long countAttendanceThisMonth() {
        return 0L; // Simplificado por ahora
    }

    private double calculateAttendanceRate() {
        return 0.0; // Simplificado por ahora
    }

    private List<AnalyticsDTO.AttendanceByDay> getAttendanceByDay() {
        return new java.util.ArrayList<>(); // Simplificado por ahora
    }
}
