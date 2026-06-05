package com.example.gym.repository;

import com.example.gym.model.Subscription;
import com.example.gym.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUser(User user);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.plan WHERE s.user.id = :userId AND s.status = 'ACTIVE' ORDER BY s.startDate DESC")
    Optional<Subscription> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.plan.id = :planId AND s.status = 'ACTIVE'")
    boolean existsActiveByPlanId(@Param("planId") Long planId);
}
