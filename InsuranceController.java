package com.uip.backend.controller;

import com.uip.backend.model.CashFlow;
import com.uip.backend.model.InsurancePolicy;
import com.uip.backend.model.Loan;
import com.uip.backend.model.User;
import com.uip.backend.repository.CashFlowRepository;
import com.uip.backend.repository.InsuranceRepository;
import com.uip.backend.repository.LoanRepository;
import com.uip.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/insurance")
public class InsuranceController {

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CashFlowRepository cashFlowRepository;

    @Autowired
    private LoanRepository loanRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InsurancePolicy>> getUserPolicies(@PathVariable Long userId) {
        List<InsurancePolicy> policies = insuranceRepository.findByUserId(userId);
        return ResponseEntity.ok(policies);
    }

    @PostMapping
    public ResponseEntity<?> createPolicy(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String policyName = (String) payload.get("policyName");
            String policyType = (String) payload.get("policyType");
            String provider = (String) payload.get("provider");
            String policyNumber = payload.get("policyNumber") != null ? payload.get("policyNumber").toString() : null;
            Double sumAssured = Double.valueOf(payload.get("sumAssured").toString());
            Double premiumAmount = Double.valueOf(payload.get("premiumAmount").toString());
            String premiumFrequency = payload.get("premiumFrequency") != null ? payload.get("premiumFrequency").toString() : "ANNUAL";
            
            LocalDate startDate = payload.get("startDate") != null && !payload.get("startDate").toString().isEmpty()
                    ? LocalDate.parse(payload.get("startDate").toString()) : LocalDate.now();
            LocalDate renewalDate = payload.get("renewalDate") != null && !payload.get("renewalDate").toString().isEmpty()
                    ? LocalDate.parse(payload.get("renewalDate").toString()) : LocalDate.now().plusYears(1);
            LocalDate expiryDate = payload.get("expiryDate") != null && !payload.get("expiryDate").toString().isEmpty()
                    ? LocalDate.parse(payload.get("expiryDate").toString()) : null;

            String nominee = payload.get("nominee") != null ? payload.get("nominee").toString() : null;
            String status = payload.get("status") != null ? payload.get("status").toString() : "ACTIVE";
            String taxBenefitSection = payload.get("taxBenefitSection") != null ? payload.get("taxBenefitSection").toString() : 
                    (policyType.startsWith("LIFE") ? "80C" : (policyType.contains("HEALTH") ? "80D" : "NONE"));
            String notes = payload.get("notes") != null ? payload.get("notes").toString() : null;

            InsurancePolicy policy = InsurancePolicy.builder()
                    .user(user)
                    .policyName(policyName)
                    .policyType(policyType)
                    .provider(provider)
                    .policyNumber(policyNumber)
                    .sumAssured(sumAssured)
                    .premiumAmount(premiumAmount)
                    .premiumFrequency(premiumFrequency)
                    .startDate(startDate)
                    .renewalDate(renewalDate)
                    .expiryDate(expiryDate)
                    .nominee(nominee)
                    .status(status)
                    .taxBenefitSection(taxBenefitSection)
                    .notes(notes)
                    .build();

            InsurancePolicy saved = insuranceRepository.save(policy);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to save insurance policy: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePolicy(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            InsurancePolicy policy = insuranceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            if (payload.containsKey("policyName")) policy.setPolicyName(payload.get("policyName").toString());
            if (payload.containsKey("policyType")) policy.setPolicyType(payload.get("policyType").toString());
            if (payload.containsKey("provider")) policy.setProvider(payload.get("provider").toString());
            if (payload.containsKey("policyNumber")) policy.setPolicyNumber(payload.get("policyNumber").toString());
            if (payload.containsKey("sumAssured")) policy.setSumAssured(Double.valueOf(payload.get("sumAssured").toString()));
            if (payload.containsKey("premiumAmount")) policy.setPremiumAmount(Double.valueOf(payload.get("premiumAmount").toString()));
            if (payload.containsKey("premiumFrequency")) policy.setPremiumFrequency(payload.get("premiumFrequency").toString());
            if (payload.containsKey("renewalDate")) policy.setRenewalDate(LocalDate.parse(payload.get("renewalDate").toString()));
            if (payload.containsKey("nominee")) policy.setNominee(payload.get("nominee").toString());
            if (payload.containsKey("status")) policy.setStatus(payload.get("status").toString());
            if (payload.containsKey("taxBenefitSection")) policy.setTaxBenefitSection(payload.get("taxBenefitSection").toString());
            if (payload.containsKey("notes")) policy.setNotes(payload.get("notes").toString());

            InsurancePolicy updated = insuranceRepository.save(policy);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to update insurance policy: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePolicy(@PathVariable Long id) {
        if (!insuranceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        insuranceRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Insurance policy deleted successfully"));
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getInsuranceSummary(@PathVariable Long userId) {
        List<InsurancePolicy> policies = insuranceRepository.findByUserId(userId);
        LocalDate today = LocalDate.now();

        double totalSumAssured = 0.0;
        double lifeCoverSumAssured = 0.0;
        double healthCoverSumAssured = 0.0;
        double motorCoverSumAssured = 0.0;
        double otherCoverSumAssured = 0.0;
        double totalAnnualPremium = 0.0;
        int activeCount = 0;

        List<Map<String, Object>> upcomingRenewals = new ArrayList<>();

        for (InsurancePolicy p : policies) {
            if ("ACTIVE".equalsIgnoreCase(p.getStatus()) || "DUE_SOON".equalsIgnoreCase(p.getStatus())) {
                activeCount++;
                double sum = p.getSumAssured() != null ? p.getSumAssured() : 0.0;
                totalSumAssured += sum;

                String type = p.getPolicyType() != null ? p.getPolicyType().toUpperCase() : "OTHER";
                if (type.contains("LIFE")) {
                    lifeCoverSumAssured += sum;
                } else if (type.contains("HEALTH") || type.contains("CRITICAL")) {
                    healthCoverSumAssured += sum;
                } else if (type.contains("MOTOR")) {
                    motorCoverSumAssured += sum;
                } else {
                    otherCoverSumAssured += sum;
                }

                // Annualize premium
                double prem = p.getPremiumAmount() != null ? p.getPremiumAmount() : 0.0;
                String freq = p.getPremiumFrequency() != null ? p.getPremiumFrequency().toUpperCase() : "ANNUAL";
                if ("MONTHLY".equals(freq)) {
                    totalAnnualPremium += prem * 12.0;
                } else if ("QUARTERLY".equals(freq)) {
                    totalAnnualPremium += prem * 4.0;
                } else if ("HALF_YEARLY".equals(freq)) {
                    totalAnnualPremium += prem * 2.0;
                } else {
                    totalAnnualPremium += prem;
                }

                // Check renewal timing
                if (p.getRenewalDate() != null) {
                    long daysUntilRenewal = ChronoUnit.DAYS.between(today, p.getRenewalDate());
                    Map<String, Object> rMap = new HashMap<>();
                    rMap.put("id", p.getId());
                    rMap.put("policyName", p.getPolicyName());
                    rMap.put("provider", p.getProvider());
                    rMap.put("policyType", p.getPolicyType());
                    rMap.put("premiumAmount", p.getPremiumAmount());
                    rMap.put("renewalDate", p.getRenewalDate().toString());
                    rMap.put("daysRemaining", daysUntilRenewal);
                    rMap.put("isOverdue", daysUntilRenewal < 0);
                    rMap.put("isDueSoon", daysUntilRenewal >= 0 && daysUntilRenewal <= 45);
                    upcomingRenewals.add(rMap);
                }
            }
        }

        upcomingRenewals.sort(Comparator.comparingLong(a -> (Long) a.get("daysRemaining")));

        // Protection Adequacy Calculations (Human Life Value - HLV Method)
        List<CashFlow> flows = cashFlowRepository.findByUserIdOrderByFlowDateDesc(userId);
        double totalInflow = flows.stream()
                .filter(f -> "INCOME".equalsIgnoreCase(f.getType()))
                .mapToDouble(CashFlow::getAmount)
                .sum();
        
        // If cashflow has records, annualize; else provide reasonable baseline (e.g. 10L)
        double annualIncome = totalInflow > 0 ? (totalInflow >= 100000 ? totalInflow : totalInflow * 12.0) : 1200000.0;

        List<Loan> loans = loanRepository.findByUserIdAndStatus(userId, "ACTIVE");
        double totalActiveDebt = loans.stream()
                .mapToDouble(l -> l.getPrincipalAmount() - (l.getAmountPaid() != null ? l.getAmountPaid() : 0.0))
                .sum();

        // Standard Indian Rule of Thumb: 10x - 15x Annual Income + Total Outstanding Loans
        double recommendedLifeCover = Math.max(5000000.0, (12.0 * annualIncome) + totalActiveDebt);
        double lifeCoverPct = recommendedLifeCover > 0 ? Math.min(100.0, (lifeCoverSumAssured / recommendedLifeCover) * 100.0) : 0.0;
        String lifeStatus = lifeCoverPct >= 100 ? "EXCELLENT" : (lifeCoverPct >= 60 ? "ADEQUATE" : (lifeCoverPct >= 30 ? "UNDERINSURED" : "CRITICAL_GAP"));

        // Health Benchmark: Minimum ₹15,00,000 for family coverage
        double recommendedHealthCover = 1500000.0;
        double healthCoverPct = Math.min(100.0, (healthCoverSumAssured / recommendedHealthCover) * 100.0);
        String healthStatus = healthCoverPct >= 100 ? "OPTIMAL" : (healthCoverPct >= 50 ? "MODERATE" : "VULNERABLE");

        // Overall Protection Score (0 to 100)
        double protectionScore = (lifeCoverPct * 0.6) + (healthCoverPct * 0.4);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSumAssured", Math.round(totalSumAssured));
        summary.put("lifeCoverSumAssured", Math.round(lifeCoverSumAssured));
        summary.put("healthCoverSumAssured", Math.round(healthCoverSumAssured));
        summary.put("motorCoverSumAssured", Math.round(motorCoverSumAssured));
        summary.put("otherCoverSumAssured", Math.round(otherCoverSumAssured));
        summary.put("totalAnnualPremium", Math.round(totalAnnualPremium));
        summary.put("activePoliciesCount", activeCount);
        summary.put("upcomingRenewals", upcomingRenewals);

        Map<String, Object> adequacy = new HashMap<>();
        adequacy.put("annualIncome", Math.round(annualIncome));
        adequacy.put("totalActiveDebt", Math.round(totalActiveDebt));
        adequacy.put("recommendedLifeCover", Math.round(recommendedLifeCover));
        adequacy.put("currentLifeCover", Math.round(lifeCoverSumAssured));
        adequacy.put("lifeCoveragePct", Math.round(lifeCoverPct));
        adequacy.put("lifeStatus", lifeStatus);
        adequacy.put("lifeGap", Math.round(Math.max(0, recommendedLifeCover - lifeCoverSumAssured)));

        adequacy.put("recommendedHealthCover", Math.round(recommendedHealthCover));
        adequacy.put("currentHealthCover", Math.round(healthCoverSumAssured));
        adequacy.put("healthCoveragePct", Math.round(healthCoverPct));
        adequacy.put("healthStatus", healthStatus);
        adequacy.put("healthGap", Math.round(Math.max(0, recommendedHealthCover - healthCoverSumAssured)));

        adequacy.put("overallProtectionScore", Math.round(protectionScore));

        summary.put("adequacy", adequacy);

        return ResponseEntity.ok(summary);
    }
}
