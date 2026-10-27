package com.uip.backend.controller;

import com.uip.backend.model.Investment;
import com.uip.backend.model.Portfolio;
import com.uip.backend.model.Transaction;
import com.uip.backend.model.User;
import com.uip.backend.repository.UserRepository;
import com.uip.backend.service.PortfolioService;
import com.uip.backend.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Portfolio>> getPortfolios(@PathVariable Long userId) {
        List<Portfolio> portfolios = portfolioService.getPortfoliosByUserId(userId);
        return ResponseEntity.ok(portfolios);
    }

    @PostMapping
    public ResponseEntity<?> createPortfolio(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String name = (String) payload.get("name");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .name(name)
                .build();

        Portfolio saved = portfolioService.createPortfolio(portfolio);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{portfolioId}/holdings")
    public ResponseEntity<List<Investment>> getHoldings(@PathVariable Long portfolioId) {
        List<Investment> holdings = portfolioService.getInvestmentsByPortfolioId(portfolioId);
        return ResponseEntity.ok(holdings);
    }

    @PostMapping("/{portfolioId}/transaction")
    public ResponseEntity<?> executeTransaction(@PathVariable Long portfolioId, 
                                                @RequestBody Map<String, Object> payload) {
        try {
            if (payload == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Transaction payload is required."));
            }
            String assetName = payload.get("assetName") != null ? payload.get("assetName").toString() : (payload.get("name") != null ? payload.get("name").toString() : "");
            String assetType = payload.get("assetType") != null ? payload.get("assetType").toString() : "EQUITY";
            String buySell = payload.get("buySell") != null ? payload.get("buySell").toString() : (payload.get("type") != null ? payload.get("type").toString() : "BUY");
            Double quantity = Double.valueOf(payload.get("quantity").toString());
            Double price = Double.valueOf(payload.get("price").toString());

            Transaction tx = portfolioService.executeTransaction(portfolioId, assetName, assetType, buySell, quantity, price);
            return ResponseEntity.ok(tx);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Transaction execution failed."));
        }
    }

    @GetMapping("/{portfolioId}/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(@PathVariable Long portfolioId) {
        List<Transaction> transactions = portfolioService.getTransactionsByPortfolioId(portfolioId);
        return ResponseEntity.ok(transactions);
    }

    @Autowired
    private MarketDataService marketDataService;

    @GetMapping("/market-quotes")
    public ResponseEntity<List<Map<String, Object>>> getMarketQuotes() {
        return ResponseEntity.ok(marketDataService.getLiveMarketQuotes());
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getUserPortfolioSummary(@PathVariable Long userId) {
        List<Portfolio> portfolios = portfolioService.getPortfoliosByUserId(userId);
        double totalValue = 0.0;
        double totalInvested = 0.0;
        int holdingCount = 0;
        Map<String, Double> assetBreakdown = new HashMap<>();

        for (Portfolio p : portfolios) {
            List<Investment> holdings = portfolioService.getInvestmentsByPortfolioId(p.getId());
            for (Investment h : holdings) {
                if (h.getQuantity() > 0) {
                    holdingCount++;
                    double curVal = h.getQuantity() * h.getCurrentPrice();
                    double buyP = h.getBuyPrice() != null ? h.getBuyPrice() : h.getCurrentPrice();
                    double invested = h.getQuantity() * buyP;
                    totalValue += curVal;
                    totalInvested += invested;

                    String type = h.getAssetType() != null ? h.getAssetType() : "OTHER";
                    assetBreakdown.put(type, assetBreakdown.getOrDefault(type, 0.0) + curVal);
                }
            }
        }

        double pnl = totalValue - totalInvested;
        double pnlPct = totalInvested > 0 ? (pnl / totalInvested) * 100.0 : 0.0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("portfoliosCount", portfolios.size());
        summary.put("totalValue", Math.round(totalValue * 100.0) / 100.0);
        summary.put("totalInvested", Math.round(totalInvested * 100.0) / 100.0);
        summary.put("unrealizedPnL", Math.round(pnl * 100.0) / 100.0);
        summary.put("unrealizedPnLPct", Math.round(pnlPct * 100.0) / 100.0);
        summary.put("totalHoldingsCount", holdingCount);
        summary.put("assetBreakdown", assetBreakdown);

        return ResponseEntity.ok(summary);
    }
}
