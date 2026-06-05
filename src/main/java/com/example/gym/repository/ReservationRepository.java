package com.example.gym.repository;

import com.example.gym.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    // Buscar reservas confirmadas de un usuario con JOIN FETCH para evitar lazy loading
    @Query("SELECT r FROM Reservation r JOIN FETCH r.user u JOIN FETCH r.schedule s WHERE r.user.id = :userId AND r.status = 'CONFIRMED' ORDER BY r.date DESC, s.startTime")
    List<Reservation> findByUserIdAndConfirmedStatus(@Param("userId") Long userId);
    
    // Buscar reservas confirmadas por fecha con JOIN FETCH
    @Query("SELECT r FROM Reservation r JOIN FETCH r.user u JOIN FETCH r.schedule s WHERE r.date = :date AND r.status = 'CONFIRMED' ORDER BY s.startTime")
    List<Reservation> findByDateAndConfirmedStatus(@Param("date") LocalDate date);
    
    // Contar reservas confirmadas para un horario específico en una fecha
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.schedule.id = :scheduleId AND r.date = :date AND r.status = 'CONFIRMED'")
    Long countConfirmedReservationsByScheduleAndDate(@Param("scheduleId") Long scheduleId, @Param("date") LocalDate date);
    
    // Verificar si un usuario ya tiene reserva confirmada para un horario y fecha específicos
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND r.schedule.id = :scheduleId AND r.date = :date")
    Long countByUserAndScheduleAndDate(@Param("userId") Long userId, @Param("scheduleId") Long scheduleId, @Param("date") LocalDate date);
    
    // Verificar si un usuario ya tiene reserva CONFIRMADA para un horario y fecha específicos (fix para reservas canceladas)
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND r.schedule.id = :scheduleId AND r.date = :date AND r.status = 'CONFIRMED'")
    Long countConfirmedByUserAndScheduleAndDate(@Param("userId") Long userId, @Param("scheduleId") Long scheduleId, @Param("date") LocalDate date);

    // Verificar si el staff (TRAINER/OWNER) canceló ese mismo turno
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND r.schedule.id = :scheduleId AND r.date = :date AND r.status = 'CANCELLED' AND r.cancelledByUserId IS NOT NULL")
    Long countStaffCancelledByUserAndScheduleAndDate(@Param("userId") Long userId, @Param("scheduleId") Long scheduleId, @Param("date") LocalDate date);
    
    // Buscar todas las reservas confirmadas con JOIN FETCH
    @Query("SELECT r FROM Reservation r JOIN FETCH r.user u JOIN FETCH r.schedule s WHERE r.status = 'CONFIRMED' ORDER BY r.date DESC, s.startTime")
    List<Reservation> findAllConfirmedReservations();

    // Buscar todas las reservas (cualquier status) asociadas a un schedule
    List<Reservation> findByScheduleId(Long scheduleId);

    // Buscar reservas por schedule y status
    List<Reservation> findByScheduleIdAndStatus(Long scheduleId, Reservation.ReservationStatus status);

    // Buscar reservas por schedule, fecha y status
    List<Reservation> findByScheduleIdAndDateAndStatus(Long scheduleId, LocalDate date, Reservation.ReservationStatus status);

    // Contar no-shows (reservas no asistidas) dentro de una ventana de fechas
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND r.status = 'CONFIRMED' AND r.attended = false AND r.date BETWEEN :startDate AND :endDate")
    Long countNoShowsInRange(@Param("userId") Long userId,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate);

    // Obtener reservas futuras (incluyendo hoy) confirmadas de un usuario
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.status = 'CONFIRMED' AND r.date >= :startDate")
    List<Reservation> findFutureConfirmedByUser(@Param("userId") Long userId,
                                                @Param("startDate") LocalDate startDate);

    // Contar asistencias efectivas (attended=true) en una semana para un usuario
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND r.attended = true AND r.date BETWEEN :weekStart AND :weekEnd")
    Long countAttendedInWeek(@Param("userId") Long userId,
                             @Param("weekStart") LocalDate weekStart,
                             @Param("weekEnd") LocalDate weekEnd);
}
