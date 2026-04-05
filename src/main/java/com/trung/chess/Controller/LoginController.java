package com.trung.chess.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller; 
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.trung.chess.Repository.MatchRepository;
import com.trung.chess.Repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    
    @PostMapping("/login-submit")
    public Object login(@RequestParam("username") String user, 
                        @RequestParam("password") String pass, 
                        @RequestHeader(value = "Accept", required = false) String acceptHeader,
                        HttpSession session) {
        
        
        boolean wantsJson = (acceptHeader != null && acceptHeader.contains("application/json"));

        if (userRepository.checkLogin(user, pass)) {
            session.setAttribute("user", user); 
            
            if (wantsJson) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Đăng nhập thành công");
                response.put("user", user);
                return ResponseEntity.ok(response);
            }
            return "redirect:/Dashboard.html"; 
        } else {
            if (wantsJson) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Sai tên đăng nhập hoặc mật khẩu");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            return "redirect:/Login.html?error=true"; 
        }
    }

    
    @PostMapping("/save-match")
    @ResponseBody 
    public ResponseEntity<Map<String, Object>> saveMatch(@RequestParam String result,
                                                        @RequestParam String opponent,
                                                        @RequestParam(required = false) String reason,
                                                        @RequestParam(required = false) String pgn,
                                                        HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String currentUser = (String) session.getAttribute("user");

        if (currentUser != null) {
            
            matchRepository.saveGameHistoryWithPgn(null, currentUser, opponent, result, reason, pgn);
            response.put("status", "success");
            response.put("message", "Đã lưu kết quả ván đấu");
            return ResponseEntity.ok(response);
        }

        
        matchRepository.incrementGuestMatch();
        response.put("status", "error");
        response.put("message", "Người dùng chưa đăng nhập");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    
@PostMapping("/logout")
public Object logout(HttpSession session, 
                      @RequestHeader(value = "Accept", required = false) String acceptHeader) {
    
    
    session.invalidate(); 

    boolean wantsJson = (acceptHeader != null && acceptHeader.contains("application/json"));
    
    if (wantsJson) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã đăng xuất thành công");
        return ResponseEntity.ok(response);
    }
    
    return "redirect:/Login.html"; 
}
}
