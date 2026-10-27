package com.uip.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    private String phone;
    
    @Column(name = "risk_profile")
    private String riskProfile; // CONSERVATIVE, MODERATE, AGGRESSIVE, UNASSESSED

    @Column(name = "cibil_score")
    @Builder.Default
    private Integer cibilScore = 765; // CIBIL Credit Score (300 to 900)

    @Column(nullable = false)
    @Builder.Default
    private String role = "CLIENT"; // CLIENT, ADMIN

    @Builder.Default
    private String currency = "INR"; // INR, USD

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = java.time.LocalDateTime.now();
        }
        if (role == null) {
            role = "CLIENT";
        }
        if (currency == null) {
            currency = "INR";
        }
        if (cibilScore == null) {
            cibilScore = 765;
        }
    }
}
