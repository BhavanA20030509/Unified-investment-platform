package com.uip.backend.repository;

import com.uip.backend.model.CashFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CashFlowRepository extends JpaRepository<CashFlow, Long> {
    List<CashFlow> findByUserIdOrderByFlowDateDesc(Long userId);
    List<CashFlow> findByUserIdAndType(Long userId, String type);
    List<CashFlow> findByUserIdAndFlowDateBetweenOrderByFlowDateDesc(Long userId, LocalDate startDate, LocalDate endDate);
}
