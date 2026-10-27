package com.uip.backend.controller;

import com.uip.backend.model.Loan;
import com.uip.backend.model.User;
import com.uip.backend.repository.LoanRepository;
import com.uip.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Loan>> getUserLoans(@PathVariable Long userId) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        return ResponseEntity.ok(loans);
    }

    @PostMapping
    public ResponseEntity<?> createLoan(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String loanName = (String) payload.get("loanName");
        String loanType = (String) payload.get("loanType"); // HOME, CAR, PERSONAL, EDUCATION, etc.
        Double principal = Double.valueOf(payload.get("principalAmount").toString());
        Double interestRate = Double.valueOf(payload.get("interestRate").toString());
        Integer tenureMonths = Integer.valueOf(payload.get("tenureMonths").toString());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Calculate EMI formula: P * r * (1+r)^n / ((1+r)^n - 1)
        double r = (interestRate / 1200.0);
        double emi = (principal * r * Math.pow(1 + r, tenureMonths)) / (Math.pow(1 + r, tenureMonths) - 1);
        emi = Math.round(emi * 100.0) / 100.0;

        Double amountPaid = payload.containsKey("amountPaid") && payload.get("amountPaid") != null 
                ? Double.valueOf(payload.get("amountPaid").toString()) : 0.0;

        String startDateStr = (String) payload.get("startDate");
        LocalDate startDate = (startDateStr != null && !startDateStr.isEmpty()) 
                ? LocalDate.parse(startDateStr) 
                : LocalDate.now();

        Loan loan = Loan.builder()
                .user(user)
                .loanName(loanName)
                .loanType(loanType.toUpperCase())
                .principalAmount(principal)
                .interestRate(interestRate)
                .tenureMonths(tenureMonths)
                .emiAmount(emi)
                .amountPaid(amountPaid)
                .startDate(startDate)
                .nextEmiDate(startDate.plusMonths(1))
                .status("ACTIVE")
                .build();

        Loan saved = loanRepository.save(loan);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLoan(@PathVariable Long id) {
        if (!loanRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        loanRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Loan record deleted successfully"));
    }

    /**
     * Complete month-by-month Amortization Schedule
     */
    @GetMapping("/{id}/amortization")
    public ResponseEntity<?> getAmortizationSchedule(@PathVariable Long id) {
        Optional<Loan> opt = loanRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Loan loan = opt.get();
        double p = loan.getPrincipalAmount();
        double r = (loan.getInterestRate() / 1200.0);
        int n = loan.getTenureMonths();
        double emi = loan.getEmiAmount();
        LocalDate paymentDate = loan.getStartDate() != null ? loan.getStartDate() : LocalDate.now();

        List<Map<String, Object>> schedule = new ArrayList<>();
        double balance = p;
        double totalInterestPaid = 0.0;
        double totalPrincipalPaid = 0.0;

        for (int month = 1; month <= n && balance > 0.01; month++) {
            double interestForMonth = balance * r;
            double principalForMonth = emi - interestForMonth;
            if (principalForMonth > balance) {
                principalForMonth = balance;
                emi = principalForMonth + interestForMonth;
            }
            balance -= principalForMonth;
            totalInterestPaid += interestForMonth;
            totalPrincipalPaid += principalForMonth;
            paymentDate = paymentDate.plusMonths(1);

            Map<String, Object> entry = new HashMap<>();
            entry.put("month", month);
            entry.put("paymentDate", paymentDate.toString());
            entry.put("beginningBalance", Math.round((balance + principalForMonth) * 100.0) / 100.0);
            entry.put("emi", Math.round(emi * 100.0) / 100.0);
            entry.put("principalComponent", Math.round(principalForMonth * 100.0) / 100.0);
            entry.put("interestComponent", Math.round(interestForMonth * 100.0) / 100.0);
            entry.put("endingBalance", Math.max(0, Math.round(balance * 100.0) / 100.0));
            entry.put("totalInterestPaidToDate", Math.round(totalInterestPaid * 100.0) / 100.0);
            schedule.add(entry);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("loan", loan);
        result.put("remainingBalance", Math.max(0, loan.getPrincipalAmount() - (loan.getAmountPaid() != null ? loan.getAmountPaid() : 0.0)));
        result.put("totalInterestPayable", Math.round(totalInterestPaid * 100.0) / 100.0);
        result.put("totalCostOfLoan", Math.round((loan.getPrincipalAmount() + totalInterestPaid) * 100.0) / 100.0);
        result.put("schedule", schedule);

        return ResponseEntity.ok(result);
    }

    /**
     * Mark an EMI as paid
     */
    @PostMapping("/{id}/pay-emi")
    public ResponseEntity<?> payEmi(@PathVariable Long id) {
        Optional<Loan> opt = loanRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Loan loan = opt.get();
        double paid = loan.getAmountPaid() != null ? loan.getAmountPaid() : 0.0;
        paid += loan.getEmiAmount();
        loan.setAmountPaid(paid);

        if (loan.getNextEmiDate() != null) {
            loan.setNextEmiDate(loan.getNextEmiDate().plusMonths(1));
        }

        if (paid >= loan.getPrincipalAmount()) {
            loan.setStatus("CLOSED");
        }

        Loan saved = loanRepository.save(loan);
        return ResponseEntity.ok(saved);
    }
}
