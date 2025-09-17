package com.example.gym.repository;

import com.example.gym.model.Subscription;
import com.example.gym.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUser(User user);
    Optional<Subscription> findByUserAndStatus(User user, Subscription.Status status);
}
