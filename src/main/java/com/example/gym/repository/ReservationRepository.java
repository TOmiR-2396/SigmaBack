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
    
    // Buscar todas las reservas confirmadas con JOIN FETCH
    @Query("SELECT r FROM Reservation r JOIN FETCH r.user u JOIN FETCH r.schedule s WHERE r.status = 'CONFIRMED' ORDER BY r.date DESC, s.startTime")
    List<Reservation> findAllConfirmedReservations();

    // Buscar todas las reservas (cualquier status) asociadas a un schedule
    List<Reservation> findByScheduleId(Long scheduleId);

    // Buscar reservas por schedule y status
    List<Reservation> findByScheduleIdAndStatus(Long scheduleId, Reservation.ReservationStatus status);


}
