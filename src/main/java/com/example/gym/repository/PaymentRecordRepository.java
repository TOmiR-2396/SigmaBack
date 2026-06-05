package com.example.gym.repository;

import com.example.gym.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    @Query("SELECT r FROM PaymentRecord r LEFT JOIN FETCH r.plan LEFT JOIN FETCH r.registeredBy WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<PaymentRecord> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    List<PaymentRecord> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentRecord.PaymentStatus status);

    // Pagos aprobados de un mes/año — usa paymentDate si está disponible, sino createdAt
    @Query("SELECT r FROM PaymentRecord r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.plan LEFT JOIN FETCH r.registeredBy " +
           "WHERE r.status = 'APPROVED' AND r.amount > 0 AND (" +
           "  (r.paymentDate IS NOT NULL AND YEAR(r.paymentDate) = :year AND MONTH(r.paymentDate) = :month) OR " +
           "  (r.paymentDate IS NULL AND YEAR(r.createdAt) = :year AND MONTH(r.createdAt) = :month)" +
           ") ORDER BY r.user.lastName, r.user.firstName")
    List<PaymentRecord> findApprovedByMonth(@Param("year") int year, @Param("month") int month);
}
