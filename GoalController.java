package com.uip.backend.controller;

import com.uip.backend.model.Goal;
import com.uip.backend.model.Portfolio;
import com.uip.backend.model.User;
import com.uip.backend.repository.GoalRepository;
import com.uip.backend.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private com.uip.backend.repository.UserRepository userRepository;

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<Goal>> getGoals(@PathVariable Long portfolioId) {
        List<Goal> goals = goalRepository.findByPortfolioId(portfolioId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Goal>> getGoalsByUserId(@PathVariable Long userId) {
        List<Portfolio> ports = portfolioRepository.findByUserId(userId);
        List<Goal> allGoals = new java.util.ArrayList<>();
        for (Portfolio p : ports) {
            allGoals.addAll(goalRepository.findByPortfolioId(p.getId()));
        }
        return ResponseEntity.ok(allGoals);
    }

    @PostMapping
    public ResponseEntity<?> createGoal(@RequestBody Map<String, Object> payload) {
        try {
            Long portfolioId = null;
            if (payload.get("portfolioId") != null && !payload.get("portfolioId").toString().isEmpty()) {
                try {
                    portfolioId = Long.valueOf(payload.get("portfolioId").toString());
                } catch (Exception ignored) {}
            }

            Long userId = null;
            if (payload.get("userId") != null && !payload.get("userId").toString().isEmpty()) {
                try {
                    userId = Long.valueOf(payload.get("userId").toString());
                } catch (Exception ignored) {}
            }

            Portfolio portfolio = null;
            if (portfolioId != null) {
                portfolio = portfolioRepository.findById(portfolioId).orElse(null);
            }

            if (portfolio == null && userId != null) {
                List<Portfolio> userPorts = portfolioRepository.findByUserId(userId);
                if (!userPorts.isEmpty()) {
                    portfolio = userPorts.get(0);
                } else {
                    final Long uid = userId;
                    User u = userRepository.findById(uid).orElseThrow(() -> new RuntimeException("User not found"));
                    portfolio = portfolioRepository.save(Portfolio.builder().user(u).name("Main Investment Portfolio").totalValue(0.0).build());
                }
            }

            if (portfolio == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "A valid portfolio or user must be specified to link this goal."));
            }

            String name = (String) payload.get("goalName");
            Double targetAmount = Double.valueOf(payload.get("targetAmount").toString());
            LocalDate targetDate = LocalDate.parse((String) payload.get("targetDate"));

            Goal goal = Goal.builder()
                    .portfolio(portfolio)
                    .goalName(name)
                    .targetAmount(targetAmount)
                    .targetDate(targetDate)
                    .status("ACTIVE")
                    .build();

            Goal saved = goalRepository.save(goal);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to save goal: " + e.getMessage()));
        }
    }

    @PutMapping("/{goalId}/status")
    public ResponseEntity<?> updateGoalStatus(@PathVariable Long goalId, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        Optional<Goal> goalOpt = goalRepository.findById(goalId);
        if (goalOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Goal goal = goalOpt.get();
        goal.setStatus(status);
        Goal saved = goalRepository.save(goal);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long goalId) {
        if (!goalRepository.existsById(goalId)) {
            return ResponseEntity.notFound().build();
        }
        goalRepository.deleteById(goalId);
        return ResponseEntity.ok(Map.of("message", "Goal deleted successfully"));
    }
}
