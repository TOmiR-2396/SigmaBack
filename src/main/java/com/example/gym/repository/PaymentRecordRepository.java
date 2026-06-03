package com.example.gym.repository;

import com.example.gym.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    List<PaymentRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PaymentRecord> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentRecord.PaymentStatus status);
}
