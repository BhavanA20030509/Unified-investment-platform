package com.uip.backend.controller;

import com.uip.backend.model.Investment;
import com.uip.backend.model.Portfolio;
import com.uip.backend.model.User;
import com.uip.backend.repository.InvestmentRepository;
import com.uip.backend.repository.PortfolioRepository;
import com.uip.backend.repository.UserRepository;
import com.uip.backend.service.RiskAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    @Autowired
    private RiskAnalysisService riskAnalysisService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> analyzeUserRisk(@PathVariable Long userId) {
        // Find all portfolios for user
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        List<Investment> allHoldings = new ArrayList<>();
        
        for (Portfolio p : portfolios) {
            allHoldings.addAll(investmentRepository.findByPortfolioId(p.getId()));
        }

        Map<String, Object> analysis = riskAnalysisService.analyzePortfolioRisk(userId, allHoldings);
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<?> updateRiskProfile(@PathVariable Long userId, @RequestBody Map<String, String> payload) {
        String riskProfile = payload.get("riskProfile"); // CONSERVATIVE, MODERATE, AGGRESSIVE
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setRiskProfile(riskProfile.toUpperCase());
        User savedUser = userRepository.save(user);
        
        return ResponseEntity.ok(savedUser);
    }
}
