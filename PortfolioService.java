package com.uip.backend.service;

import com.uip.backend.model.Investment;
import com.uip.backend.model.Portfolio;
import com.uip.backend.model.Transaction;
import com.uip.backend.repository.InvestmentRepository;
import com.uip.backend.repository.PortfolioRepository;
import com.uip.backend.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private com.uip.backend.repository.UserRepository userRepository;

    public List<Portfolio> getPortfoliosByUserId(Long userId) {
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        if (portfolios.isEmpty()) {
            userRepository.findById(userId).ifPresent(user -> {
                Portfolio defaultPort = Portfolio.builder()
                        .user(user)
                        .name("Main Investment Portfolio")
                        .totalValue(0.0)
                        .build();
                portfolioRepository.save(defaultPort);
            });
            portfolios = portfolioRepository.findByUserId(userId);
        }
        for (Portfolio p : portfolios) {
            recalculatePortfolioValue(p.getId());
        }
        return portfolioRepository.findByUserId(userId);
    }

    public Portfolio createPortfolio(Portfolio portfolio) {
        portfolio.setTotalValue(0.0);
        return portfolioRepository.save(portfolio);
    }

    public List<Investment> getInvestmentsByPortfolioId(Long portfolioId) {
        return investmentRepository.findByPortfolioId(portfolioId);
    }

    @Transactional
    public Transaction executeTransaction(Long portfolioId, String assetName, String assetType, 
                                          String buySell, Double quantity, Double price) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        String cleanAssetName = assetName != null ? assetName.trim().toUpperCase() : "UNKNOWN";
        String cleanAssetType = (assetType != null && !assetType.trim().isEmpty()) ? assetType.trim().toUpperCase() : "STOCK";
        String cleanBuySell = (buySell != null && !buySell.trim().isEmpty()) ? buySell.trim().toUpperCase() : "BUY";

        // Try to find existing investment holding in this portfolio (case-insensitive)
        Investment investment = investmentRepository.findByPortfolioId(portfolioId).stream()
                .filter(i -> i.getAssetName() != null && i.getAssetName().equalsIgnoreCase(cleanAssetName))
                .findFirst()
                .orElse(null);

        if (cleanBuySell.equalsIgnoreCase("BUY")) {
            if (investment == null) {
                // Create new holding
                investment = Investment.builder()
                        .portfolio(portfolio)
                        .assetName(cleanAssetName)
                        .assetType(cleanAssetType)
                        .quantity(quantity)
                        .buyPrice(price)
                        .investedAmount(quantity * price)
                        .currentPrice(price) // Initial price is the buy price
                        .build();
            } else {
                // Update existing holding quantity, average buy price, and current price
                double oldQty = investment.getQuantity() != null ? investment.getQuantity() : 0.0;
                double oldInvested = investment.getInvestedAmount() != null ? investment.getInvestedAmount() : (oldQty * (investment.getBuyPrice() != null ? investment.getBuyPrice() : price));
                double newInvested = oldInvested + (quantity * price);
                double newQty = oldQty + quantity;

                investment.setQuantity(newQty);
                investment.setInvestedAmount(newInvested);
                investment.setBuyPrice(newQty > 0 ? (newInvested / newQty) : price);
                investment.setCurrentPrice(price);
            }
            investment = investmentRepository.save(investment);
        } else if (cleanBuySell.equalsIgnoreCase("SELL")) {
            if (investment == null || investment.getQuantity() < quantity) {
                throw new RuntimeException("Insufficient holdings to execute sell transaction. Current holdings: " + (investment != null ? investment.getQuantity() : 0) + " units.");
            }
            double oldQty = investment.getQuantity();
            double avgBuyPrice = investment.getBuyPrice() != null ? investment.getBuyPrice() : investment.getCurrentPrice();
            double remainingQty = oldQty - quantity;

            investment.setQuantity(remainingQty);
            investment.setInvestedAmount(remainingQty * avgBuyPrice);
            investment.setCurrentPrice(price);
            investment = investmentRepository.save(investment);
        } else {
            throw new RuntimeException("Invalid transaction type: " + cleanBuySell);
        }

        // Create transaction log
        Transaction transaction = Transaction.builder()
                .portfolio(portfolio)
                .investment(investment)
                .buySell(cleanBuySell)
                .quantity(quantity)
                .price(price)
                .transactionDate(LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);

        // Recalculate portfolio total value
        recalculatePortfolioValue(portfolioId);

        return transaction;
    }

    public void recalculatePortfolioValue(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        
        List<Investment> holdings = investmentRepository.findByPortfolioId(portfolioId);
        double total = 0.0;
        for (Investment h : holdings) {
            total += h.getQuantity() * h.getCurrentPrice();
        }
        
        portfolio.setTotalValue(total);
        portfolioRepository.save(portfolio);
    }

    public List<Transaction> getTransactionsByPortfolioId(Long portfolioId) {
        return transactionRepository.findByPortfolioId(portfolioId);
    }
}
