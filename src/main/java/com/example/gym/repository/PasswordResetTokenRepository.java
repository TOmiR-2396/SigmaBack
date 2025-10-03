package com.example.gym.repository;

import com.example.gym.model.PasswordResetToken;
import com.example.gym.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    // Buscar token por valor
    Optional<PasswordResetToken> findByToken(String token);
    
    // Buscar token por valor incluyendo el usuario (para evitar lazy loading)
    @Query("SELECT p FROM PasswordResetToken p JOIN FETCH p.user WHERE p.token = :token")
    Optional<PasswordResetToken> findByTokenWithUser(@Param("token") String token);
    
    // Buscar tokens válidos para un usuario
    @Query("SELECT p FROM PasswordResetToken p WHERE p.user = :user AND p.used = false AND p.expiryDate > :now")
    List<PasswordResetToken> findValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    // Marcar todos los tokens de un usuario como usados
    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken p SET p.used = true WHERE p.user = :user AND p.used = false")
    void invalidateAllUserTokens(@Param("user") User user);
    
    // Eliminar tokens expirados (para limpieza periódica)
    @Modifying
    @Transactional
    void deleteByExpiryDateBefore(LocalDateTime date);
}