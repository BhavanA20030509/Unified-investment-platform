package com.uip.backend.controller;

import com.uip.backend.model.User;
import com.uip.backend.model.Watchlist;
import com.uip.backend.repository.UserRepository;
import com.uip.backend.repository.WatchlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Watchlist>> getWatchlist(@PathVariable Long userId) {
        List<Watchlist> watchlists = watchlistRepository.findByUserId(userId);
        return ResponseEntity.ok(watchlists);
    }

    @PostMapping
    public ResponseEntity<?> addToWatchlist(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String assetName = (String) payload.get("assetName");
        String assetType = (String) payload.get("assetType");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Watchlist item = Watchlist.builder()
                .user(user)
                .assetName(assetName)
                .assetType(assetType.toUpperCase())
                .build();

        Watchlist saved = watchlistRepository.save(item);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeFromWatchlist(@PathVariable Long id) {
        if (!watchlistRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        watchlistRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Watchlist item removed"));
    }
}
