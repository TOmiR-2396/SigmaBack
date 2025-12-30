package com.example.gym.model;

import com.example.gym.tenant.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(name = "uq_role_tenant_name", columnNames = {"tenant_id", "name"})
})
@Filter(name = "tenantFilter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleName name;

    public enum RoleName {
        MEMBER,   // Rol por defecto
        TRAINER,  // Asignado por OWNER
        OWNER     // Encargado de asignar TRAINERS
    }

    public RoleName getName() {
        return name;
    }
}
