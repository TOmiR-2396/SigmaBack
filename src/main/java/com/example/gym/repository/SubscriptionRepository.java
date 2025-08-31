package com.example.gym.repository;

import com.example.gym.model.Subscription;
import com.example.gym.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByMember(Member member);
}
