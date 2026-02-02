    // ...existing code...
package com.example.gym.model;

import com.example.gym.tenant.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_tenant_email", columnNames = {"tenant_id", "email"})
})
@Filter(name = "tenantFilter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends TenantEntity {
    public Long getId() {
        return id;
    }
    public enum UserRole { MEMBER, TRAINER, OWNER }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.MEMBER;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    private String phone;

    private LocalDate birthDate;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate joinDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.INACTIVE;

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    private LocalDateTime deactivatedAt;

    @Column
    private Long deactivatedByUserId;

    @Column
    private String deactivatedByRole;

    @Column
    private String deactivationReason;

    @Column(nullable = false)
    private String password;

    @Column(length = 500)
    private String trainingPlanUrl;

    // Eliminado Set<Role> roles

    public enum UserStatus { ACTIVE, INACTIVE, SUSPENDED }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }
    public void setRole(UserRole role) {
        this.role = role;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public UserStatus getStatus() {
        return status;
    }
    
    public void setStatus(UserStatus status) {
        this.status = status;
    }
}