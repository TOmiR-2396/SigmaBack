package com.example.gym.model;

import com.example.gym.tenant.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Filter(name = "tenantFilter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken extends TenantEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;
    
    // Método para verificar si el token ha expirado
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
    // Método para verificar si el token es válido
    public boolean isValid() {
        return !used && !isExpired();
    }
}