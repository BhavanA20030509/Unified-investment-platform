package com.uip.backend.controller;

import com.uip.backend.model.*;
import com.uip.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private SavingsPlanRepository savingsPlanRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private CashFlowRepository cashFlowRepository;

    @GetMapping("/user/{userId}/comprehensive")
    public ResponseEntity<?> getComprehensiveReport(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // 1. Portfolios & Investments
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        double totalInvested = 0.0;
        double currentPortfolioValue = 0.0;
        Map<String, Double> assetAllocation = new HashMap<>();

        List<Map<String, Object>> holdingsReport = new ArrayList<>();
        for (Portfolio p : portfolios) {
            List<Investment> holdings = investmentRepository.findByPortfolioId(p.getId());
            for (Investment h : holdings) {
                if (h.getQuantity() > 0) {
                    double invested = (h.getBuyPrice() != null ? h.getBuyPrice() : h.getCurrentPrice()) * h.getQuantity();
                    double curVal = h.getCurrentPrice() * h.getQuantity();
                    double pnl = curVal - invested;
                    double pnlPct = invested > 0 ? (pnl / invested) * 100.0 : 0.0;

                    totalInvested += invested;
                    currentPortfolioValue += curVal;

                    String type = h.getAssetType() != null ? h.getAssetType() : "OTHER";
                    assetAllocation.put(type, assetAllocation.getOrDefault(type, 0.0) + curVal);

                    Map<String, Object> item = new HashMap<>();
                    item.put("assetName", h.getAssetName());
                    item.put("assetType", type);
                    item.put("quantity", h.getQuantity());
                    item.put("buyPrice", h.getBuyPrice() != null ? h.getBuyPrice() : h.getCurrentPrice());
                    item.put("currentPrice", h.getCurrentPrice());
                    item.put("investedAmount", Math.round(invested * 100.0) / 100.0);
                    item.put("currentValue", Math.round(curVal * 100.0) / 100.0);
                    item.put("pnl", Math.round(pnl * 100.0) / 100.0);
                    item.put("pnlPercentage", Math.round(pnlPct * 100.0) / 100.0);
                    holdingsReport.add(item);
                }
            }
        }

        double totalPnL = currentPortfolioValue - totalInvested;
        double totalPnLPct = totalInvested > 0 ? (totalPnL / totalInvested) * 100.0 : 0.0;

        // 2. Savings Plans
        List<SavingsPlan> savingsPlans = savingsPlanRepository.findByUserId(userId);
        double totalSavingsBalance = 0.0;
        double totalMonthlyAdditions = 0.0;
        for (SavingsPlan sp : savingsPlans) {
            totalSavingsBalance += sp.getInitialDeposit();
            totalMonthlyAdditions += sp.getMonthlyAddition();
        }

        // 3. Loans & Liabilities
        List<Loan> loans = loanRepository.findByUserId(userId);
        double totalDebtRemaining = 0.0;
        double totalMonthlyEmiOutflow = 0.0;
        for (Loan l : loans) {
            if ("ACTIVE".equalsIgnoreCase(l.getStatus())) {
                double remaining = l.getPrincipalAmount() - (l.getAmountPaid() != null ? l.getAmountPaid() : 0.0);
                totalDebtRemaining += Math.max(0, remaining);
                totalMonthlyEmiOutflow += l.getEmiAmount();
            }
        }

        // 4. Cash Flows
        List<CashFlow> flows = cashFlowRepository.findByUserIdOrderByFlowDateDesc(userId);
        double totalIncome = 0.0;
        double totalExpenses = 0.0;
        Map<String, Double> expensesByCategory = new HashMap<>();

        for (CashFlow cf : flows) {
            if ("INCOME".equalsIgnoreCase(cf.getType())) {
                totalIncome += cf.getAmount();
            } else {
                totalExpenses += cf.getAmount();
                expensesByCategory.put(cf.getCategory(), expensesByCategory.getOrDefault(cf.getCategory(), 0.0) + cf.getAmount());
            }
        }

        // 5. Goals
        List<Goal> goals = new ArrayList<>();
        for (Portfolio p : portfolios) {
            goals.addAll(goalRepository.findByPortfolioId(p.getId()));
        }

        // 6. Net Worth
        double netWorth = (currentPortfolioValue + totalSavingsBalance) - totalDebtRemaining;

        Map<String, Object> report = new HashMap<>();
        report.put("clientName", user.getName());
        report.put("clientEmail", user.getEmail());
        report.put("riskProfile", user.getRiskProfile());
        report.put("netWorth", Math.round(netWorth * 100.0) / 100.0);
        report.put("currentPortfolioValue", Math.round(currentPortfolioValue * 100.0) / 100.0);
        report.put("totalInvested", Math.round(totalInvested * 100.0) / 100.0);
        report.put("totalPnL", Math.round(totalPnL * 100.0) / 100.0);
        report.put("totalPnLPercentage", Math.round(totalPnLPct * 100.0) / 100.0);
        report.put("totalSavingsBalance", Math.round(totalSavingsBalance * 100.0) / 100.0);
        report.put("totalMonthlyAdditions", Math.round(totalMonthlyAdditions * 100.0) / 100.0);
        report.put("totalDebtRemaining", Math.round(totalDebtRemaining * 100.0) / 100.0);
        report.put("totalMonthlyEmiOutflow", Math.round(totalMonthlyEmiOutflow * 100.0) / 100.0);
        report.put("totalIncome", Math.round(totalIncome * 100.0) / 100.0);
        report.put("totalExpenses", Math.round(totalExpenses * 100.0) / 100.0);
        report.put("netCashSavings", Math.round((totalIncome - totalExpenses) * 100.0) / 100.0);
        report.put("assetAllocation", assetAllocation);
        report.put("expensesByCategory", expensesByCategory);
        report.put("holdings", holdingsReport);
        report.put("goals", goals);
        report.put("loans", loans);
        report.put("savingsPlans", savingsPlans);
        report.put("generatedAt", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(report);
    }
}
