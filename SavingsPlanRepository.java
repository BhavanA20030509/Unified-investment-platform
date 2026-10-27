package com.uip.backend.repository;

import com.uip.backend.model.SavingsPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsPlanRepository extends JpaRepository<SavingsPlan, Long> {
    List<SavingsPlan> findByUserId(Long userId);
}
