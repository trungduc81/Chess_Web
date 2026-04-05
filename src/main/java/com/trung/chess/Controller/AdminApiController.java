package com.trung.chess.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trung.chess.Repository.MatchRepository;
import com.trung.chess.Repository.UserRepository;

import jakarta.servlet.http.HttpSession;


@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> getAdminSession(HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "admin", isAdmin(session),
            "username", session.getAttribute("user")
        ));
    }

    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        Map<String, Object> stats = new HashMap<>();
        long totalMatches = matchRepository.countAllMatches();
        long guestMatches = matchRepository.countGuestMatches();

        stats.put("totalMatches", totalMatches);                  
        stats.put("todayMatches", matchRepository.countTodayMatches());
        stats.put("totalUsers", userRepository.countAllUsers());
        stats.put("guestMatches", guestMatches);                  
        stats.put("totalMatchesAll", totalMatches + guestMatches); 
        return ResponseEntity.ok(stats);
    }

    
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("users", userRepository.findAll());
        
        return ResponseEntity.ok(response);
    }

    
    @PostMapping("/users/lock/{id}")
    public ResponseEntity<Map<String, String>> lockUser(@PathVariable Long id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        Map<String, String> response = new HashMap<>();
        boolean locked = userRepository.lockUser(id);

        if (locked) {
            response.put("status", "success");
            response.put("message", "User locked successfully");
            return ResponseEntity.ok(response);
        }

        response.put("status", "error");
        response.put("message", "Cannot lock user");
        return ResponseEntity.badRequest().body(response);
    }

    
    @PostMapping("/users/unlock/{id}")
    public ResponseEntity<Map<String, String>> unlockUser(@PathVariable Long id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        Map<String, String> response = new HashMap<>();
        boolean unlocked = userRepository.unlockUser(id);

        if (unlocked) {
            response.put("status", "success");
            response.put("message", "User unlocked successfully");
            return ResponseEntity.ok(response);
        }

        response.put("status", "error");
        response.put("message", "Cannot unlock user");
        return ResponseEntity.badRequest().body(response);
    }

    
    @GetMapping("/users/{id}/matches")
    public ResponseEntity<Map<String, Object>> getUserMatches(@PathVariable Integer id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        String username = userRepository.findUsernameById(id);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        List<Map<String, Object>> matches = matchRepository.getHistoryByUserId(id);
        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("matches", matches);
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/matches/{id}")
    public ResponseEntity<Map<String, Object>> getMatchDetail(@PathVariable Long id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        Map<String, Object> match = matchRepository.getMatchByIdForAdmin(id);
        if (match.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Match not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("match", match);
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/recent-matches")
    public ResponseEntity<?> getRecentMatches(HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        List<Map<String, Object>> matches = matchRepository.getRecentMatches(20);
        return ResponseEntity.ok(matches);
    }

    
    @GetMapping("/top-players")
    public ResponseEntity<?> getTopPlayers(HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        List<Map<String, Object>> topPlayers = matchRepository.getTopPlayersByWinRate(5);
        return ResponseEntity.ok(Map.of("players", topPlayers));
    }

    
    private boolean isAuthenticated(HttpSession session) {
        Object username = session.getAttribute("user");
        return username != null && !username.toString().isBlank();
    }

    
    private boolean isAdmin(HttpSession session) {
        Object username = session.getAttribute("user");
        return username != null && "admin".equalsIgnoreCase(username.toString().trim());
    }
}

