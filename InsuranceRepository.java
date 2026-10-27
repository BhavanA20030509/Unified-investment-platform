package com.uip.backend.repository;

import com.uip.backend.model.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsuranceRepository extends JpaRepository<InsurancePolicy, Long> {
    List<InsurancePolicy> findByUserId(Long userId);
    List<InsurancePolicy> findByUserIdAndPolicyType(Long userId, String policyType);
    List<InsurancePolicy> findByUserIdAndStatus(Long userId, String status);
}
