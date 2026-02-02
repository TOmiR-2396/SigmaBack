package com.example.gym.tenant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenant_switches", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tenant_switch", columnNames = {"tenant_id", "switch_key"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSwitch extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "switch_key", nullable = false, length = 100)
    private String key;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "payload", length = 2000)
    private String payload; // JSON opcional con metadata
}
