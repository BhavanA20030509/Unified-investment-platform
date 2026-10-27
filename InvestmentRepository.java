package com.uip.backend.repository;

import com.uip.backend.model.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByPortfolioId(Long portfolioId);
    Optional<Investment> findByPortfolioIdAndAssetName(Long portfolioId, String assetName);
}
