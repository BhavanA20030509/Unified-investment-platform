package com.uip.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "insurance_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "policy_name", nullable = false)
    private String policyName; // e.g. "HDFC Life Click 2 Protect 3D", "Star Health Comprehensive"

    @Column(name = "policy_type", nullable = false)
    private String policyType; // LIFE_TERM, HEALTH, MOTOR, HOME, CRITICAL_ILLNESS, TRAVEL, OTHER

    @Column(name = "provider", nullable = false)
    private String provider; // e.g. "HDFC Life", "Star Health", "LIC", "ICICI Lombard", "Tata AIG"

    @Column(name = "policy_number")
    private String policyNumber; // e.g. "POL-782910"

    @Column(name = "sum_assured", nullable = false)
    private Double sumAssured; // e.g. 10000000 (1 Crore) for Life, 1000000 (10 Lakh) for Health

    @Column(name = "premium_amount", nullable = false)
    private Double premiumAmount; // e.g. 16500

    @Column(name = "premium_frequency", nullable = false)
    @Builder.Default
    private String premiumFrequency = "ANNUAL"; // ANNUAL, MONTHLY, QUARTERLY, HALF_YEARLY

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "nominee")
    private String nominee; // e.g. "Spouse", "Parents", "Child"

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, DUE_SOON, LAPSED, MATURED

    @Column(name = "tax_benefit_section")
    private String taxBenefitSection; // 80C (Life), 80D (Health), NONE

    @Column(name = "notes")
    private String notes;
}
