package com.uip.backend.service;

import com.uip.backend.model.Investment;
import com.uip.backend.model.User;
import com.uip.backend.repository.InvestmentRepository;
import com.uip.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiskAnalysisService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    public Map<String, Object> analyzePortfolioRisk(Long userId, List<Investment> holdings) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String profile = user.getRiskProfile();
        if (profile == null || profile.isEmpty()) {
            profile = "UNASSESSED";
        }

        // Establish Targets
        Map<String, Double> targets = getTargetAllocation(profile);

        // Calculate actual allocation
        Map<String, Double> actualValues = new HashMap<>();
        actualValues.put("STOCKS", 0.0);
        actualValues.put("MUTUAL_FUNDS", 0.0);
        actualValues.put("ETFS", 0.0);
        actualValues.put("BONDS", 0.0);
        actualValues.put("CRYPTO", 0.0);

        double totalValue = 0.0;
        for (Investment h : holdings) {
            double value = h.getQuantity() * h.getCurrentPrice();
            totalValue += value;
            
            String type = h.getAssetType().toUpperCase();
            if (type.contains("STOCK")) {
                actualValues.put("STOCKS", actualValues.get("STOCKS") + value);
            } else if (type.contains("MUTUAL")) {
                actualValues.put("MUTUAL_FUNDS", actualValues.get("MUTUAL_FUNDS") + value);
            } else if (type.contains("ETF")) {
                actualValues.put("ETFS", actualValues.get("ETFS") + value);
            } else if (type.contains("BOND")) {
                actualValues.put("BONDS", actualValues.get("BONDS") + value);
            } else {
                actualValues.put("CRYPTO", actualValues.get("CRYPTO") + value);
            }
        }

        Map<String, Double> actualWeights = new HashMap<>();
        for (Map.Entry<String, Double> entry : actualValues.entrySet()) {
            double weight = (totalValue > 0) ? (entry.getValue() / totalValue) * 100.0 : 0.0;
            actualWeights.put(entry.getKey(), Math.round(weight * 100.0) / 100.0);
        }

        // Create rebalancing recommendations
        StringBuilder recommendation = new StringBuilder();
        if (profile.equals("UNASSESSED")) {
            recommendation.append("You have not completed your risk assessment yet. Please complete the questionnaire in the Risk Analysis tab to receive tailored investment advice.");
        } else {
            recommendation.append("Based on your ").append(profile).append(" risk profile, ");
            boolean needsRebalancing = false;
            
            for (String assetClass : targets.keySet()) {
                double target = targets.get(assetClass);
                double actual = actualWeights.getOrDefault(assetClass, 0.0);
                
                // If weight deviates by more than 10% from target
                if (Math.abs(actual - target) > 10.0) {
                    needsRebalancing = true;
                    if (actual > target) {
                        recommendation.append("your allocation in ").append(assetClass.toLowerCase().replace("_", " "))
                                .append(" (").append(actual).append("%) is significantly higher than your target of ")
                                .append(target).append("%. Consider selling some of these holdings. ");
                    } else {
                        recommendation.append("you are underweight in ").append(assetClass.toLowerCase().replace("_", " "))
                                .append(" (").append(actual).append("%) compared to your target of ")
                                .append(target).append("%. Consider buying more to build stability. ");
                    }
                }
            }
            if (!needsRebalancing) {
                recommendation.append("your portfolio is well-balanced and aligns perfectly with your financial goals and volatility tolerance. Keep it up!");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("riskProfile", profile);
        result.put("totalValue", totalValue);
        result.put("targetAllocation", targets);
        result.put("actualAllocation", actualWeights);
        result.put("recommendation", recommendation.toString());

        return result;
    }

    public Map<String, Double> getTargetAllocation(String riskProfile) {
        Map<String, Double> targets = new HashMap<>();
        switch (riskProfile.toUpperCase()) {
            case "CONSERVATIVE":
                targets.put("STOCKS", 20.0);
                targets.put("MUTUAL_FUNDS", 20.0);
                targets.put("ETFS", 15.0);
                targets.put("BONDS", 40.0);
                targets.put("CRYPTO", 5.0);
                break;
            case "AGGRESSIVE":
                targets.put("STOCKS", 60.0);
                targets.put("MUTUAL_FUNDS", 10.0);
                targets.put("ETFS", 10.0);
                targets.put("BONDS", 5.0);
                targets.put("CRYPTO", 15.0);
                break;
            case "MODERATE":
            default:
                targets.put("STOCKS", 40.0);
                targets.put("MUTUAL_FUNDS", 20.0);
                targets.put("ETFS", 20.0);
                targets.put("BONDS", 15.0);
                targets.put("CRYPTO", 5.0);
                break;
        }
        return targets;
    }
}
