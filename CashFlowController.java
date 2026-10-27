package com.uip.backend.controller;

import com.uip.backend.model.CashFlow;
import com.uip.backend.model.User;
import com.uip.backend.repository.CashFlowRepository;
import com.uip.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/cashflow")
public class CashFlowController {

    @Autowired
    private CashFlowRepository cashFlowRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CashFlow>> getUserCashFlows(@PathVariable Long userId) {
        List<CashFlow> list = cashFlowRepository.findByUserIdOrderByFlowDateDesc(userId);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<?> createCashFlow(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String type = (String) payload.get("type"); // INCOME, EXPENSE
        String category = (String) payload.get("category");
        Double amount = Double.valueOf(payload.get("amount").toString());
        String flowDateStr = (String) payload.get("flowDate");
        String description = (String) payload.get("description");
        String paymentMode = (String) payload.get("paymentMode");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate flowDate = (flowDateStr != null && !flowDateStr.isEmpty()) 
                ? LocalDate.parse(flowDateStr) 
                : LocalDate.now();

        CashFlow flow = CashFlow.builder()
                .user(user)
                .type(type.toUpperCase())
                .category(category)
                .amount(amount)
                .flowDate(flowDate)
                .description(description)
                .paymentMode(paymentMode != null ? paymentMode : "UPI")
                .build();

        CashFlow saved = cashFlowRepository.save(flow);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCashFlow(@PathVariable Long id) {
        if (!cashFlowRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        cashFlowRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Record deleted successfully"));
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getCashFlowSummary(@PathVariable Long userId) {
        List<CashFlow> all = cashFlowRepository.findByUserIdOrderByFlowDateDesc(userId);

        double totalIncome = 0.0;
        double totalExpense = 0.0;
        Map<String, Double> expenseByCategory = new HashMap<>();
        Map<String, Double> incomeByCategory = new HashMap<>();
        Map<String, Double> monthlyIncome = new TreeMap<>();
        Map<String, Double> monthlyExpense = new TreeMap<>();

        for (CashFlow cf : all) {
            double amt = cf.getAmount();
            String monthKey = cf.getFlowDate().getYear() + "-" + String.format("%02d", cf.getFlowDate().getMonthValue());

            if ("INCOME".equalsIgnoreCase(cf.getType())) {
                totalIncome += amt;
                incomeByCategory.put(cf.getCategory(), incomeByCategory.getOrDefault(cf.getCategory(), 0.0) + amt);
                monthlyIncome.put(monthKey, monthlyIncome.getOrDefault(monthKey, 0.0) + amt);
            } else {
                totalExpense += amt;
                expenseByCategory.put(cf.getCategory(), expenseByCategory.getOrDefault(cf.getCategory(), 0.0) + amt);
                monthlyExpense.put(monthKey, monthlyExpense.getOrDefault(monthKey, 0.0) + amt);
            }
        }

        double netSavings = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100.0 : 0.0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpenses", totalExpense);
        summary.put("netSavings", netSavings);
        summary.put("savingsRatePct", Math.round(savingsRate * 10.0) / 10.0);
        summary.put("expenseByCategory", expenseByCategory);
        summary.put("incomeByCategory", incomeByCategory);
        summary.put("monthlyIncome", monthlyIncome);
        summary.put("monthlyExpense", monthlyExpense);
        summary.put("totalTransactions", all.size());

        return ResponseEntity.ok(summary);
    }
}
