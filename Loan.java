package com.uip.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "loan_name", nullable = false)
    private String loanName; // e.g. "HDFC Car Loan", "SBI Home Loan", "Education Loan"

    @Column(name = "loan_type", nullable = false)
    private String loanType; // HOME, CAR, PERSONAL, EDUCATION, GOLD, OTHER

    @Column(name = "principal_amount", nullable = false)
    private Double principalAmount; // e.g. 800000

    @Column(name = "interest_rate", nullable = false)
    private Double interestRate; // Annual % e.g. 8.5

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths; // e.g. 60 (5 years)

    @Column(name = "emi_amount", nullable = false)
    private Double emiAmount;

    @Column(name = "amount_paid")
    @Builder.Default
    private Double amountPaid = 0.0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "next_emi_date")
    private LocalDate nextEmiDate;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, CLOSED
}
