package com.uip.backend.controller;

import com.uip.backend.model.User;
import com.uip.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.uip.backend.repository.PortfolioRepository portfolioRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email address is required."));
        }
        
        String cleanEmail = user.getEmail().trim().toLowerCase();
        user.setEmail(cleanEmail);

        if (userRepository.findByEmailIgnoreCase(cleanEmail).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use."));
        }
        if (user.getRiskProfile() == null) {
            user.setRiskProfile("UNASSESSED");
        }
        if (user.getCibilScore() == null) {
            user.setCibilScore(765);
        }
        User savedUser = userRepository.save(user);

        // Auto-provision initial portfolio for the newly registered client
        com.uip.backend.model.Portfolio defaultPort = com.uip.backend.model.Portfolio.builder()
                .user(savedUser)
                .name("Main Investment Portfolio")
                .totalValue(0.0)
                .build();
        portfolioRepository.save(defaultPort);

        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password."));
        }

        String cleanEmail = email.trim();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);
        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(password)) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password."));
        }

        return ResponseEntity.ok(userOpt.get());
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userOpt.get());
    }
}
