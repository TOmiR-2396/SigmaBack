package com.example.gym.repository;

import com.example.gym.model.Payment;
import com.example.gym.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPreferenceId(String preferenceId);

    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByUserOrderByCreatedAtDesc(User user);

    List<Payment> findByStatusOrderByCreatedAtDesc(Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.externalReference = :externalReference")
    Optional<Payment> findByExternalReference(@Param("externalReference") String externalReference);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.user = :user AND p.status = 'APPROVED'")
    Long countApprovedPaymentsByUser(@Param("user") User user);

    @Query("SELECT p FROM Payment p WHERE p.status IN ('PENDING', 'IN_PROCESS') AND p.createdAt < :expiredTime")
    List<Payment> findExpiredPendingPayments(@Param("expiredTime") LocalDateTime expiredTime);
}
