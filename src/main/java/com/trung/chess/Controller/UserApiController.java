package com.trung.chess.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trung.chess.Repository.MatchRepository;

import jakarta.servlet.http.HttpSession;


@RestController
@RequestMapping("/api")
public class UserApiController {

    @Autowired
    private MatchRepository matchRepository;

    
    @GetMapping("/user/session")
    public ResponseEntity<Map<String, Object>> getUserSession(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("user");
        
        response.put("authenticated", username != null);
        response.put("username", username != null ? username : "Guest");
        
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/user/match-history")
    public ResponseEntity<Map<String, Object>> getMatchHistory(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("user");
        
        if (username == null) {
            response.put("authenticated", false);
            response.put("history", List.of());
            return ResponseEntity.ok(response);
        }
        
        response.put("authenticated", true);
        response.put("username", username);
        response.put("history", matchRepository.getHistory(username));
        
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/user/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            response.put("authenticated", false);
            response.put("totalMatches", 0);
            response.put("totalWins", 0);
            response.put("winRate", 0.0);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> stats = matchRepository.getUserStats(username);
        response.put("authenticated", true);
        response.put("username", username);
        response.put("totalMatches", stats.getOrDefault("totalMatches", 0));
        response.put("totalWins", stats.getOrDefault("totalWins", 0));
        response.put("winRate", stats.getOrDefault("winRate", 0.0));
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/user/latest-match-history")
    public ResponseEntity<Map<String, Object>> getLatestMatchHistory(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            response.put("authenticated", false);
            response.put("history", Map.of());
            return ResponseEntity.ok(response);
        }

        response.put("authenticated", true);
        response.put("username", username);
        response.put("history", matchRepository.getLatestHistory(username));

        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/user/match/{id}")
    public ResponseEntity<Map<String, Object>> getMatchById(@PathVariable Long id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            response.put("authenticated", false);
            response.put("match", Map.of());
            return ResponseEntity.ok(response);
        }

        response.put("authenticated", true);
        response.put("match", matchRepository.getMatchById(id, username));

        return ResponseEntity.ok(response);
    }
}

