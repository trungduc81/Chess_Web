package com.trung.chess.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trung.chess.Repository.UserRepository;


@RestController
@RequestMapping("/api/forgot-password")
public class ForgotPasswordController {

    @Autowired
    private UserRepository userRepository;

    
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");

        
        if (username == null || username.isBlank() || email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "verified", false,
                "message", "Vui lòng nhập đầy đủ thông tin!"
            ));
        }

        
        boolean verified = userRepository.verifyUsernameEmail(username, email);

        if (verified) {
            return ResponseEntity.ok(Map.of("verified", true));
        } else {
            return ResponseEntity.ok(Map.of(
                "verified", false,
                "message", "Tên đăng nhập hoặc email không chính xác!"
            ));
        }
    }

    
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String newPassword = body.get("newPassword");

        
        if (username == null || username.isBlank() ||
            email == null || email.isBlank() ||
            newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Vui lòng nhập đầy đủ thông tin!"
            ));
        }

        
        if (newPassword.length() < 3) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Mật khẩu phải có ít nhất 3 ký tự!"
            ));
        }

        
        boolean success = userRepository.resetPassword(username, email, newPassword);

        if (success) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đổi mật khẩu thành công!"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Không thể đặt lại mật khẩu. Vui lòng thử lại!"
            ));
        }
    }
}

