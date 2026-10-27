package com.uip.backend.controller;

import com.uip.backend.service.AISimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AISimulationService aiSimulationService;

    @PostMapping("/chat")
    public ResponseEntity<?> askChatbot(@RequestBody(required = false) Map<String, Object> payload,
                                        @RequestHeader(value = "X-Gemini-Key", required = false) String geminiApiKey) {
        try {
            if (payload == null) {
                return ResponseEntity.ok(Map.of("response", "Please provide a query for the AI Advisor."));
            }

            Long userId = 1L;
            if (payload.get("userId") != null) {
                try {
                    userId = Long.valueOf(payload.get("userId").toString());
                } catch (Exception ignored) {}
            }

            String message = payload.get("message") != null ? payload.get("message").toString() : "";
            
            // Check if api key was passed in request body instead of header
            if ((geminiApiKey == null || geminiApiKey.trim().isEmpty()) && payload.containsKey("geminiApiKey") && payload.get("geminiApiKey") != null) {
                geminiApiKey = payload.get("geminiApiKey").toString();
            }

            String response = aiSimulationService.generateAIResponse(userId, message, geminiApiKey);
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("response", "I encountered an error processing your request: " + e.getMessage() + ". You can ask about your portfolio, cash flow, loans, or investments."));
        }
    }
}
