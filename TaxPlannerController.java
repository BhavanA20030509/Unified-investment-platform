package com.uip.backend.controller;

import com.uip.backend.model.CashFlow;
import com.uip.backend.model.InsurancePolicy;
import com.uip.backend.model.Investment;
import com.uip.backend.model.Loan;
import com.uip.backend.model.Portfolio;
import com.uip.backend.repository.CashFlowRepository;
import com.uip.backend.repository.InsuranceRepository;
import com.uip.backend.repository.InvestmentRepository;
import com.uip.backend.repository.LoanRepository;
import com.uip.backend.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tax")
public class TaxPlannerController {

    @Autowired
    private CashFlowRepository cashFlowRepository;

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private LoanRepository loanRepository;

    @GetMapping("/user/{userId}/analysis")
    public ResponseEntity<Map<String, Object>> getTaxAnalysis(
            @PathVariable Long userId,
            @RequestParam(required = false) Double customGrossSalary) {

        // 1. Determine Annual Gross Income
        double grossSalary;
        if (customGrossSalary != null && customGrossSalary > 0) {
            grossSalary = customGrossSalary;
        } else {
            List<CashFlow> flows = cashFlowRepository.findByUserIdOrderByFlowDateDesc(userId);
            double recordedInflow = flows.stream()
                    .filter(f -> "INCOME".equalsIgnoreCase(f.getType()))
                    .mapToDouble(CashFlow::getAmount)
                    .sum();
            grossSalary = recordedInflow > 0 ? (recordedInflow >= 200000 ? recordedInflow : recordedInflow * 12.0) : 1500000.0;
        }

        // 2. Fetch Client's actual investments and policies for Old Regime deductions
        List<InsurancePolicy> policies = insuranceRepository.findByUserId(userId);
        
        // Life Insurance Premiums (Eligible for 80C)
        double lifeInsurancePremiums = policies.stream()
                .filter(p -> ("ACTIVE".equalsIgnoreCase(p.getStatus()) || "DUE_SOON".equalsIgnoreCase(p.getStatus())) && 
                        p.getPolicyType() != null && p.getPolicyType().toUpperCase().contains("LIFE"))
                .mapToDouble(p -> p.getPremiumAmount() != null ? p.getPremiumAmount() : 0.0)
                .sum();

        // Health Insurance Premiums (Eligible for 80D)
        double healthInsurancePremiums = policies.stream()
                .filter(p -> ("ACTIVE".equalsIgnoreCase(p.getStatus()) || "DUE_SOON".equalsIgnoreCase(p.getStatus())) && 
                        p.getPolicyType() != null && 
                        (p.getPolicyType().toUpperCase().contains("HEALTH") || p.getPolicyType().toUpperCase().contains("CRITICAL")))
                .mapToDouble(p -> p.getPremiumAmount() != null ? p.getPremiumAmount() : 0.0)
                .sum();

        // ELSS / Tax Saver Mutual Funds from Portfolios (Eligible for 80C)
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        double elssInvestments = 0.0;
        for (Portfolio p : portfolios) {
            List<Investment> holdings = investmentRepository.findByPortfolioId(p.getId());
            for (Investment h : holdings) {
                if (h.getAssetName() != null && (h.getAssetName().toUpperCase().contains("ELSS") || h.getAssetName().toUpperCase().contains("TAX SAVER"))) {
                    elssInvestments += (h.getInvestedAmount() != null ? h.getInvestedAmount() : 0.0);
                }
            }
        }

        // Home Loan Interest (Eligible for Section 24b up to 2,00,000)
        List<Loan> homeLoans = loanRepository.findByUserIdAndStatus(userId, "ACTIVE").stream()
                .filter(l -> l.getLoanType() != null && l.getLoanType().toUpperCase().contains("HOME"))
                .toList();
        double homeLoanInterest = 0.0;
        for (Loan hl : homeLoans) {
            double rate = hl.getInterestRate() != null ? hl.getInterestRate() / 100.0 : 0.085;
            double principal = hl.getPrincipalAmount() != null ? hl.getPrincipalAmount() : 0.0;
            homeLoanInterest += (principal * rate);
        }

        // 3. Compute Section Deductions for Old Regime
        double total80CClaimed = lifeInsurancePremiums + elssInvestments;
        double allowed80C = Math.min(150000.0, total80CClaimed); // Section 80C Cap is 1.5 Lakh

        double allowed80D = Math.min(50000.0, healthInsurancePremiums > 0 ? healthInsurancePremiums : 25000.0); // Section 80D (Self + Family + Parents)
        double allowedSec24b = Math.min(200000.0, homeLoanInterest); // Home loan interest cap
        double standardDeductionOld = 50000.0;

        double totalOldDeductions = standardDeductionOld + allowed80C + allowed80D + allowedSec24b;
        double taxableIncomeOld = Math.max(0, grossSalary - totalOldDeductions);

        // 4. Compute Tax under Old Regime
        double taxOld = computeOldRegimeTax(taxableIncomeOld);
        double cessOld = Math.round(taxOld * 0.04);
        double totalTaxOld = taxOld + cessOld;

        // 5. Compute Tax under New Regime (Budget 2024-2025)
        double standardDeductionNew = 75000.0; // Revised Budget 2024
        double taxableIncomeNew = Math.max(0, grossSalary - standardDeductionNew);
        double taxNew = computeNewRegimeTax(taxableIncomeNew);
        double cessNew = Math.round(taxNew * 0.04);
        double totalTaxNew = taxNew + cessNew;

        // 6. Tax Comparison & Recommendation
        double taxSavings = Math.abs(totalTaxOld - totalTaxNew);
        String recommendedRegime = totalTaxNew <= totalTaxOld ? "NEW_REGIME" : "OLD_REGIME";

        Map<String, Object> response = new HashMap<>();
        response.put("grossSalary", Math.round(grossSalary));
        response.put("recommendedRegime", recommendedRegime);
        response.put("taxSavings", Math.round(taxSavings));

        // Deductions breakdown
        Map<String, Object> deductionsBreakdown = new HashMap<>();
        deductionsBreakdown.put("lifeInsurancePremium", Math.round(lifeInsurancePremiums));
        deductionsBreakdown.put("elssMutualFunds", Math.round(elssInvestments));
        deductionsBreakdown.put("total80CClaimed", Math.round(total80CClaimed));
        deductionsBreakdown.put("allowed80C", Math.round(allowed80C));
        deductionsBreakdown.put("remaining80CQuota", Math.round(Math.max(0, 150000.0 - total80CClaimed)));

        deductionsBreakdown.put("healthInsurancePremium", Math.round(healthInsurancePremiums));
        deductionsBreakdown.put("allowed80D", Math.round(allowed80D));
        deductionsBreakdown.put("remaining80DQuota", Math.round(Math.max(0, 50000.0 - healthInsurancePremiums)));

        deductionsBreakdown.put("homeLoanInterest", Math.round(homeLoanInterest));
        deductionsBreakdown.put("allowedSec24b", Math.round(allowedSec24b));
        deductionsBreakdown.put("standardDeductionOld", Math.round(standardDeductionOld));
        deductionsBreakdown.put("standardDeductionNew", Math.round(standardDeductionNew));
        deductionsBreakdown.put("totalOldDeductions", Math.round(totalOldDeductions));

        response.put("deductions", deductionsBreakdown);

        // Old Regime Summary
        Map<String, Object> oldRegime = new HashMap<>();
        oldRegime.put("taxableIncome", Math.round(taxableIncomeOld));
        oldRegime.put("baseTax", Math.round(taxOld));
        oldRegime.put("cess", Math.round(cessOld));
        oldRegime.put("totalTaxLiability", Math.round(totalTaxOld));
        response.put("oldRegime", oldRegime);

        // New Regime Summary
        Map<String, Object> newRegime = new HashMap<>();
        newRegime.put("taxableIncome", Math.round(taxableIncomeNew));
        newRegime.put("baseTax", Math.round(taxNew));
        newRegime.put("cess", Math.round(cessNew));
        newRegime.put("totalTaxLiability", Math.round(totalTaxNew));
        response.put("newRegime", newRegime);

        return ResponseEntity.ok(response);
    }

    private double computeOldRegimeTax(double taxableIncome) {
        if (taxableIncome <= 500000.0) {
            // Full rebate under 87A for income up to 5L
            return 0.0;
        }

        double tax = 0.0;
        // 0 to 2.5L: Nil
        // 2.5L to 5L: 5% (12,500)
        if (taxableIncome > 250000.0) {
            tax += Math.min(250000.0, taxableIncome - 250000.0) * 0.05;
        }
        // 5L to 10L: 20% (1,00,000)
        if (taxableIncome > 500000.0) {
            tax += Math.min(500000.0, taxableIncome - 500000.0) * 0.20;
        }
        // Above 10L: 30%
        if (taxableIncome > 1000000.0) {
            tax += (taxableIncome - 1000000.0) * 0.30;
        }

        return Math.round(tax);
    }

    private double computeNewRegimeTax(double taxableIncome) {
        if (taxableIncome <= 700000.0) {
            // Full rebate under Section 87A for income up to 7L
            return 0.0;
        }

        double tax = 0.0;
        // 0 to 3L: Nil
        // 3L to 7L: 5% (20,000)
        if (taxableIncome > 300000.0) {
            tax += Math.min(400000.0, taxableIncome - 300000.0) * 0.05;
        }
        // 7L to 10L: 10% (30,000)
        if (taxableIncome > 700000.0) {
            tax += Math.min(300000.0, taxableIncome - 700000.0) * 0.10;
        }
        // 10L to 12L: 15% (30,000)
        if (taxableIncome > 1000000.0) {
            tax += Math.min(200000.0, taxableIncome - 1000000.0) * 0.15;
        }
        // 12L to 15L: 20% (60,000)
        if (taxableIncome > 1200000.0) {
            tax += Math.min(300000.0, taxableIncome - 1200000.0) * 0.20;
        }
        // Above 15L: 30%
        if (taxableIncome > 1500000.0) {
            tax += (taxableIncome - 1500000.0) * 0.30;
        }

        return Math.round(tax);
    }
}
