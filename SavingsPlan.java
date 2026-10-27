package com.uip.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "savings_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_name", nullable = false)
    private String accountName; // e.g. "HDFC High-Yield Savings", "Emergency Fund Reserve"

    @Column(name = "initial_deposit", nullable = false)
    private Double initialDeposit; // e.g. 50000

    @Column(name = "monthly_addition", nullable = false)
    private Double monthlyAddition; // e.g. 5000

    @Column(name = "interest_rate", nullable = false)
    private Double interestRate; // Annual percentage, e.g. 7.0

    @Column(name = "period_years", nullable = false)
    private Integer periodYears; // e.g. 2

    @Column(name = "compounding_frequency")
    @Builder.Default
    private String compoundingFrequency = "MONTHLY"; // MONTHLY, QUARTERLY, ANNUALLY

    @Column(name = "start_date")
    private LocalDate startDate;

    private String notes;
}
