package com.uip.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "cash_flows")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String type; // INCOME, EXPENSE

    @Column(nullable = false)
    private String category; // Salary, Business, Food & Dining, Shopping, Rent, Education, Travel, Bills, Healthcare, Other

    @Column(nullable = false)
    private Double amount;

    @Column(name = "flow_date", nullable = false)
    private LocalDate flowDate;

    private String description;

    @Column(name = "payment_mode")
    private String paymentMode; // UPI, Net Banking, Credit Card, Cash, Debit Card
}
