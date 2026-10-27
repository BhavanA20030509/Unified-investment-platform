package com.uip.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "investments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(name = "asset_type", nullable = false)
    private String assetType; // STOCK, MUTUAL_FUND, ETF, BOND, CRYPTO

    @Column(nullable = false)
    private Double quantity;

    @Column(name = "buy_price")
    private Double buyPrice; // Average purchase price

    @Column(name = "invested_amount")
    private Double investedAmount;

    @Column(name = "current_price", nullable = false)
    private Double currentPrice;

    @Column(name = "interest_rate")
    private Double interestRate; // For FD, Bonds, RD

    @Column(name = "maturity_date")
    private java.time.LocalDate maturityDate;

    @Column(name = "sip_monthly_amount")
    private Double sipMonthlyAmount; // For SIP

    private String notes;
}
