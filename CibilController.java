package com.uip.backend.controller;

import com.uip.backend.model.Loan;
import com.uip.backend.model.User;
import com.uip.backend.repository.LoanRepository;
import com.uip.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/cibil")
public class CibilController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getCibilScoreAnalysis(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        int score = user.getCibilScore() != null ? user.getCibilScore() : 765;

        // Determine Rating Category & Color
        String ratingCategory;
        String scoreColor;
        String creditHealthText;

        if (score >= 780) {
            ratingCategory = "EXCELLENT";
            scoreColor = "#00C853"; // Green
            creditHealthText = "Your CIBIL score is in the top bracket. You are eligible for pre-approved loans at lowest interest rates.";
        } else if (score >= 720) {
            ratingCategory = "GOOD";
            scoreColor = "#1A73E8"; // Blue
            creditHealthText = "Your credit profile is healthy. Most banks will easily approve loan applications.";
        } else if (score >= 650) {
            ratingCategory = "FAIR";
            scoreColor = "#FF6D00"; // Orange
            creditHealthText = "Your CIBIL score is average. Improving your payment history can boost it above 750.";
        } else {
            ratingCategory = "POOR";
            scoreColor = "#E53935"; // Red
            creditHealthText = "Your credit score needs attention. Clear overdue EMIs to prevent loan rejections.";
        }

        // Active Loans
        List<Loan> loans = loanRepository.findByUserId(userId);
        long activeLoansCount = loans.stream().filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus())).count();

        // 5 CIBIL Score Factors
        List<Map<String, Object>> factors = new ArrayList<>();

        factors.add(Map.of(
                "name", "Payment History",
                "weight", "35%",
                "status", "EXCELLENT",
                "impact", "HIGH_POSITIVE",
                "scorePct", 98,
                "description", "98% on-time EMI repayments across all loans & credit lines."
        ));

        factors.add(Map.of(
                "name", "Credit Utilization",
                "weight", "30%",
                "status", "HEALTHY",
                "impact", "POSITIVE",
                "scorePct", 24,
                "description", "24% credit limit utilization (well below the recommended 30% limit)."
        ));

        factors.add(Map.of(
                "name", "Credit History Length",
                "weight", "15%",
                "status", "GOOD",
                "impact", "MODERATE",
                "scorePct", 80,
                "description", "4.5 years of active credit history built."
        ));

        factors.add(Map.of(
                "name", "Credit Mix",
                "weight", "10%",
                "status", "BALANCED",
                "impact", "POSITIVE",
                "scorePct", 85,
                "description", "Balanced mix of secured (Home/Car) and unsecured credit."
        ));

        factors.add(Map.of(
                "name", "New Credit Inquiries",
                "weight", "10%",
                "status", "LOW_RISK",
                "impact", "LOW_IMPACT",
                "scorePct", 90,
                "description", "Only 1 hard inquiry recorded in the past 6 months."
        ));

        // Pre-approved Offers based on Score
        List<Map<String, String>> offers = new ArrayList<>();
        if (score >= 720) {
            offers.add(Map.of("type", "Home Loan", "offer", "Pre-approved up to ₹75 Lakhs @ 8.40% p.a. (Zero Processing Fee)"));
            offers.add(Map.of("type", "Car Loan", "offer", "Instant approval up to ₹15 Lakhs @ 8.70% p.a."));
            offers.add(Map.of("type", "Personal Credit", "offer", "Pre-approved Credit Line of ₹5.0 Lakhs"));
        } else {
            offers.add(Map.of("type", "Credit Builder Loan", "offer", "Secured Credit Line against Fixed Deposit to raise CIBIL score"));
        }

        // Improvement Tips
        List<String> tips = List.of(
                "Pay all EMI and credit card dues at least 3 days before the due date.",
                "Keep overall credit card utilization under 30% of total sanctioned limit.",
                "Avoid applying for multiple credit cards or loans simultaneously (reduces hard inquiries).",
                "Maintain old credit card accounts to preserve your credit age length.",
                "Check your CIBIL report quarterly to identify and dispute any erroneous entries."
        );

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("userName", user.getName());
        result.put("cibilScore", score);
        result.put("maxScore", 900);
        result.put("minScore", 300);
        result.put("ratingCategory", ratingCategory);
        result.put("scoreColor", scoreColor);
        result.put("creditHealthText", creditHealthText);
        result.put("activeLoansCount", activeLoansCount);
        result.put("factors", factors);
        result.put("preApprovedOffers", offers);
        result.put("improvementTips", tips);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/user/{userId}/update-score")
    public ResponseEntity<?> updateCibilScore(@PathVariable Long userId, @RequestBody Map<String, Integer> payload) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Integer newScore = payload.get("cibilScore");
        if (newScore == null || newScore < 300 || newScore > 900) {
            return ResponseEntity.badRequest().body(Map.of("message", "CIBIL Score must be between 300 and 900."));
        }

        User user = userOpt.get();
        user.setCibilScore(newScore);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "CIBIL Score updated successfully", "cibilScore", newScore));
    }
}
