package com.example.gym.repository;

import com.example.gym.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE (:q IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:q,'%'))) ")
    List<User> searchUsers(@Param("q") String q);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.role = 'MEMBER' AND u.status = 'ACTIVE' AND ((u.lastLoginAt IS NOT NULL AND u.lastLoginAt < :cutoffDateTime) OR (u.lastLoginAt IS NULL AND u.joinDate < :cutoffDate))")
    List<User> findInactiveCandidates(@Param("tenantId") String tenantId,
                                      @Param("cutoffDateTime") LocalDateTime cutoffDateTime,
                                      @Param("cutoffDate") LocalDate cutoffDate);

    List<User> findByTenantIdAndRole(String tenantId, User.UserRole role);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%',:name,'%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:name,'%'))) ")
    List<User> findByNameLike(@Param("tenantId") String tenantId, @Param("name") String name);
}
