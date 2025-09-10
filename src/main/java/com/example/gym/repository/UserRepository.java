package com.example.gym.repository;

import com.example.gym.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE (:q IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:q,'%'))) ")
    List<User> searchUsers(@Param("q") String q);
}
