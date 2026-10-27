package com.uip.backend.controller;

import com.uip.backend.model.SavingsPlan;
import com.uip.backend.model.User;
import com.uip.backend.repository.SavingsPlanRepository;
import com.uip.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/savings")
public class SavingsController {

    @Autowired
    private SavingsPlanRepository savingsPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SavingsPlan>> getUserSavingsPlans(@PathVariable Long userId) {
        List<SavingsPlan> plans = savingsPlanRepository.findByUserId(userId);
        return ResponseEntity.ok(plans);
    }

    @PostMapping
    public ResponseEntity<?> createSavingsPlan(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String accountName = (String) payload.get("accountName");
        Double initialDeposit = Double.valueOf(payload.get("initialDeposit").toString());
        Double monthlyAddition = Double.valueOf(payload.get("monthlyAddition").toString());
        Double interestRate = Double.valueOf(payload.get("interestRate").toString());
        Integer periodYears = Integer.valueOf(payload.get("periodYears").toString());
        String notes = (String) payload.get("notes");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SavingsPlan plan = SavingsPlan.builder()
                .user(user)
                .accountName(accountName)
                .initialDeposit(initialDeposit)
                .monthlyAddition(monthlyAddition)
                .interestRate(interestRate)
                .periodYears(periodYears)
                .startDate(LocalDate.now())
                .notes(notes)
                .build();

        SavingsPlan saved = savingsPlanRepository.save(plan);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSavingsPlan(@PathVariable Long id) {
        if (!savingsPlanRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        savingsPlanRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Savings plan deleted successfully"));
    }

    /**
     * Exact requirement from user prompt:
     * e.g. Savings: ₹50,000, Monthly addition: ₹5,000, Interest: 7%, Period: 2 years.
     * Calculates future value, total invested, interest earned, and yearly breakdown.
     */
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateSavingsGrowth(@RequestBody Map<String, Object> payload) {
        double initial = Double.valueOf(payload.get("initialDeposit").toString());
        double monthly = Double.valueOf(payload.get("monthlyAddition").toString());
        double annualRate = Double.valueOf(payload.get("interestRate").toString());
        int years = Integer.valueOf(payload.get("periodYears").toString());

        int totalMonths = years * 12;
        double monthlyRate = (annualRate / 100.0) / 12.0;

        double currentBalance = initial;
        double totalInvested = initial;

        List<Map<String, Object>> yearlyProjections = new ArrayList<>();
        List<Map<String, Object>> monthlyProjections = new ArrayList<>();

        for (int m = 1; m <= totalMonths; m++) {
            // Interest earned this month on previous balance
            double interestThisMonth = currentBalance * monthlyRate;
            currentBalance += interestThisMonth + monthly;
            totalInvested += monthly;

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", m);
            monthData.put("invested", Math.round(totalInvested * 100.0) / 100.0);
            monthData.put("interestEarned", Math.round((currentBalance - totalInvested) * 100.0) / 100.0);
            monthData.put("futureValue", Math.round(currentBalance * 100.0) / 100.0);
            monthlyProjections.add(monthData);

            if (m % 12 == 0) {
                int year = m / 12;
                Map<String, Object> yr = new HashMap<>();
                yr.put("year", year);
                yr.put("investedAmount", Math.round(totalInvested * 100.0) / 100.0);
                yr.put("interestEarned", Math.round((currentBalance - totalInvested) * 100.0) / 100.0);
                yr.put("futureValue", Math.round(currentBalance * 100.0) / 100.0);
                yearlyProjections.add(yr);
            }
        }

        double totalInterestEarned = currentBalance - totalInvested;

        Map<String, Object> result = new HashMap<>();
        result.put("initialDeposit", initial);
        result.put("monthlyAddition", monthly);
        result.put("annualInterestRate", annualRate);
        result.put("periodYears", years);
        result.put("totalInvested", Math.round(totalInvested * 100.0) / 100.0);
        result.put("estimatedFutureValue", Math.round(currentBalance * 100.0) / 100.0);
        result.put("totalInterestEarned", Math.round(totalInterestEarned * 100.0) / 100.0);
        result.put("yearlyProjections", yearlyProjections);
        result.put("monthlyProjections", monthlyProjections);

        return ResponseEntity.ok(result);
    }

    /**
     * Financial Calculator Suite:
     * - sip
     * - fd
     * - rd
     * - simple-interest
     * - compound-interest
     * - emi
     */
    @PostMapping("/calculator/{calcType}")
    public ResponseEntity<?> runCalculator(@PathVariable String calcType, @RequestBody Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();

        switch (calcType.toLowerCase()) {
            case "sip": {
                double monthly = Double.parseDouble(req.get("monthlyInvestment").toString());
                double rate = Double.parseDouble(req.get("expectedReturnRate").toString());
                int years = Integer.parseInt(req.get("tenureYears").toString());
                int n = years * 12;
                double i = (rate / 100.0) / 12.0;
                double futureVal = monthly * ((Math.pow(1 + i, n) - 1) / i) * (1 + i);
                double invested = monthly * n;
                res.put("totalInvested", Math.round(invested));
                res.put("estimatedReturns", Math.round(futureVal - invested));
                res.put("totalValue", Math.round(futureVal));
                break;
            }
            case "fd": {
                double principal = Double.parseDouble(req.get("principal").toString());
                double rate = Double.parseDouble(req.get("interestRate").toString());
                double years = Double.parseDouble(req.get("tenureYears").toString());
                // Indian banks compound quarterly (n = 4)
                double maturity = principal * Math.pow(1 + (rate / 400.0), 4 * years);
                res.put("principal", Math.round(principal));
                res.put("interestEarned", Math.round(maturity - principal));
                res.put("maturityAmount", Math.round(maturity));
                break;
            }
            case "rd": {
                double monthly = Double.parseDouble(req.get("monthlyDeposit").toString());
                double rate = Double.parseDouble(req.get("interestRate").toString());
                int months = Integer.parseInt(req.get("tenureMonths").toString());
                double i = (rate / 400.0); // quarterly compounding
                double total = 0.0;
                for (int m = 1; m <= months; m++) {
                    total += monthly * Math.pow(1 + i, (months - m + 1) / 3.0);
                }
                double invested = monthly * months;
                res.put("totalInvested", Math.round(invested));
                res.put("interestEarned", Math.round(total - invested));
                res.put("maturityAmount", Math.round(total));
                break;
            }
            case "simple-interest": {
                double p = Double.parseDouble(req.get("principal").toString());
                double r = Double.parseDouble(req.get("rate").toString());
                double t = Double.parseDouble(req.get("timeYears").toString());
                double si = (p * r * t) / 100.0;
                res.put("principal", Math.round(p));
                res.put("simpleInterest", Math.round(si));
                res.put("totalAmount", Math.round(p + si));
                break;
            }
            case "compound-interest": {
                double p = Double.parseDouble(req.get("principal").toString());
                double r = Double.parseDouble(req.get("rate").toString());
                double t = Double.parseDouble(req.get("timeYears").toString());
                int n = req.containsKey("frequency") ? Integer.parseInt(req.get("frequency").toString()) : 12; // monthly default
                double ciTotal = p * Math.pow(1 + (r / (n * 100.0)), n * t);
                res.put("principal", Math.round(p));
                res.put("compoundInterest", Math.round(ciTotal - p));
                res.put("totalAmount", Math.round(ciTotal));
                break;
            }
            case "emi": {
                double principal = Double.parseDouble(req.get("principal").toString());
                double annualRate = Double.parseDouble(req.get("interestRate").toString());
                int tenureMonths = Integer.parseInt(req.get("tenureMonths").toString());
                double r = (annualRate / 1200.0);
                double emi = (principal * r * Math.pow(1 + r, tenureMonths)) / (Math.pow(1 + r, tenureMonths) - 1);
                double totalPayment = emi * tenureMonths;
                res.put("monthlyEmi", Math.round(emi));
                res.put("principalAmount", Math.round(principal));
                res.put("totalInterest", Math.round(totalPayment - principal));
                res.put("totalPayment", Math.round(totalPayment));
                break;
            }
            default:
                return ResponseEntity.badRequest().body(Map.of("message", "Unknown calculator type: " + calcType));
        }

        return ResponseEntity.ok(res);
    }
}
