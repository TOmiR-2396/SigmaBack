package com.example.gym.model;

import com.example.gym.tenant.TenantEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "membership_plans", uniqueConstraints = {
    @UniqueConstraint(name = "uq_membership_plan_name", columnNames = {"tenant_id", "name"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Filter(name = "tenantFilter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlan extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Positive
    @Column(nullable = false)
    private Integer durationMonths;

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    @Positive
    @Column(nullable = false)
    private Double price;

        @NotNull
        @Min(1)
        @Max(5)
        @Column(nullable = false)
        private Integer daysPerWeek;

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDaysPerWeek() {
        return daysPerWeek;
    }

    public void setDaysPerWeek(Integer daysPerWeek) {
        this.daysPerWeek = daysPerWeek;
    }
}
