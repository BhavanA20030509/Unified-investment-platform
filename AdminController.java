package com.uip.backend.controller;

import com.uip.backend.model.*;
import com.uip.backend.repository.*;
import com.uip.backend.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

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

    @Autowired
    private PortfolioService portfolioService;

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getPlatformMetrics() {
        List<User> clients = userRepository.findAll().stream()
                .filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole()))
                .toList();

        double totalInvestments = 0.0;
        List<Portfolio> allPortfolios = portfolioRepository.findAll();
        for (Portfolio p : allPortfolios) {
            totalInvestments += (p.getTotalValue() != null ? p.getTotalValue() : 0.0);
        }

        double totalSavings = 0.0;
        List<SavingsPlan> allSavings = savingsPlanRepository.findAll();
        for (SavingsPlan sp : allSavings) {
            totalSavings += (sp.getInitialDeposit() != null ? sp.getInitialDeposit() : 0.0);
        }

        double totalLoans = 0.0;
        List<Loan> allLoans = loanRepository.findAll();
        for (Loan l : allLoans) {
            if ("ACTIVE".equalsIgnoreCase(l.getStatus())) {
                double remaining = l.getPrincipalAmount() - (l.getAmountPaid() != null ? l.getAmountPaid() : 0.0);
                totalLoans += Math.max(0, remaining);
            }
        }

        long totalGoals = goalRepository.count();
        double aum = totalInvestments + totalSavings;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalClients", clients.size());
        metrics.put("totalPortfolios", allPortfolios.size());
        metrics.put("assetsUnderManagement", Math.round(aum * 100.0) / 100.0);
        metrics.put("totalInvestments", Math.round(totalInvestments * 100.0) / 100.0);
        metrics.put("totalSavings", Math.round(totalSavings * 100.0) / 100.0);
        metrics.put("totalActiveDebt", Math.round(totalLoans * 100.0) / 100.0);
        metrics.put("totalGoalsTracked", totalGoals);

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/clients")
    public ResponseEntity<List<Map<String, Object>>> getAllClients() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> clientSummaries = new ArrayList<>();

        for (User user : users) {
            List<Portfolio> ports = portfolioRepository.findByUserId(user.getId());
            double userInvestments = 0.0;
            for (Portfolio p : ports) {
                userInvestments += (p.getTotalValue() != null ? p.getTotalValue() : 0.0);
            }

            List<SavingsPlan> savings = savingsPlanRepository.findByUserId(user.getId());
            double userSavings = savings.stream().mapToDouble(SavingsPlan::getInitialDeposit).sum();

            List<Loan> loans = loanRepository.findByUserId(user.getId());
            double userLoans = loans.stream()
                    .filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus()))
                    .mapToDouble(l -> l.getPrincipalAmount() - (l.getAmountPaid() != null ? l.getAmountPaid() : 0.0))
                    .sum();

            List<CashFlow> flows = cashFlowRepository.findByUserIdOrderByFlowDateDesc(user.getId());
            double totalIncome = flows.stream().filter(f -> "INCOME".equalsIgnoreCase(f.getType())).mapToDouble(CashFlow::getAmount).sum();
            double totalExpenses = flows.stream().filter(f -> "EXPENSE".equalsIgnoreCase(f.getType())).mapToDouble(CashFlow::getAmount).sum();

            double netWorth = (userInvestments + userSavings) - userLoans;

            Map<String, Object> summary = new HashMap<>();
            summary.put("id", user.getId());
            summary.put("name", user.getName());
            summary.put("email", user.getEmail());
            summary.put("phone", user.getPhone() != null ? user.getPhone() : "N/A");
            summary.put("role", user.getRole());
            summary.put("riskProfile", user.getRiskProfile());
            summary.put("cibilScore", user.getCibilScore() != null ? user.getCibilScore() : 765);
            summary.put("portfolioCount", ports.size());
            summary.put("totalInvestments", Math.round(userInvestments * 100.0) / 100.0);
            summary.put("totalSavings", Math.round(userSavings * 100.0) / 100.0);
            summary.put("totalLoans", Math.round(userLoans * 100.0) / 100.0);
            summary.put("totalIncome", Math.round(totalIncome * 100.0) / 100.0);
            summary.put("totalExpenses", Math.round(totalExpenses * 100.0) / 100.0);
            summary.put("netWorth", Math.round(netWorth * 100.0) / 100.0);
            summary.put("goalsCount", goalRepository.findAll().stream().filter(g -> ports.stream().anyMatch(p -> p.getId().equals(g.getPortfolio().getId()))).count());

            clientSummaries.add(summary);
        }

        return ResponseEntity.ok(clientSummaries);
    }

    @GetMapping("/clients/{userId}/overview")
    public ResponseEntity<?> getClientDrilldown(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        List<Investment> allHoldings = new ArrayList<>();
        for (Portfolio p : portfolios) {
            allHoldings.addAll(investmentRepository.findByPortfolioId(p.getId()));
        }

        List<SavingsPlan> savings = savingsPlanRepository.findByUserId(userId);
        List<Loan> loans = loanRepository.findByUserId(userId);
        List<CashFlow> flows = cashFlowRepository.findByUserIdOrderByFlowDateDesc(userId);

        Map<String, Object> overview = new HashMap<>();
        overview.put("user", user);
        overview.put("cibilScore", user.getCibilScore() != null ? user.getCibilScore() : 765);
        overview.put("portfolios", portfolios);
        overview.put("holdings", allHoldings);
        overview.put("savingsPlans", savings);
        overview.put("loans", loans);
        overview.put("recentCashFlows", flows.stream().limit(10).toList());

        return ResponseEntity.ok(overview);
    }

    @GetMapping("/drawback-analysis")
    public ResponseEntity<List<Map<String, Object>>> getCompetitorDrawbackAnalysis() {
        List<Map<String, Object>> analysis = new ArrayList<>();

        analysis.add(Map.of(
                "platform", "Groww",
                "strengths", "Simple UI for mutual fund SIPs and Indian stocks; beginner friendly.",
                "limitations", "No integrated income/expense budgeting; lacks loan amortization and liability tracking; savings interest projections are absent; no multi-client wealth advisory portal.",
                "ourAdvantage", "Unified Wealth bridges daily cash flow with long-term investment, provides debt amortization, CIBIL credit scoring and interest calculation in one unified ecosystem."
        ));

        analysis.add(Map.of(
                "platform", "Zerodha (Kite + Coin + Console)",
                "strengths", "Advanced charting tools; low brokerage; robust execution for active traders.",
                "limitations", "Fragmented apps (Kite for trading, Coin for mutual funds, Console for reports); zero personal budgeting; no goal-driven monthly savings computation; no loan or credit health tracking.",
                "ourAdvantage", "Single unified application without app fragmentation. Combines trading, mutual funds, personal goals, CIBIL credit tracking, and cash flow."
        ));

        analysis.add(Map.of(
                "platform", "ET Money",
                "strengths", "Decent expense tracking via SMS parser; mutual fund reports.",
                "limitations", "Does not support real-time stock trading execution; no full loan amortization schedule; no multi-client admin/advisor view.",
                "ourAdvantage", "Offers full loan payment schedules, CIBIL credit score monitoring, multi-client administrative control for financial advisors, and cross-asset portfolio allocation."
        ));

        analysis.add(Map.of(
                "platform", "INDmoney",
                "strengths", "Broad net-worth tracking including US stocks and EPF/PPF.",
                "limitations", "Overly complex UI cluttered with upsells and third-party credit offers; proprietary lock-in; lacks customizable savings compounding math engines and clear offline transparency.",
                "ourAdvantage", "Clean, clutter-free glassmorphic interface, complete privacy, multi-role client/advisor access, transparent CIBIL credit scoring, and transparent mathematical financial engines."
        ));

        return ResponseEntity.ok(analysis);
    }
}
