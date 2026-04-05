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

import com.trung.chess.Repository.UserRepository;


@Controller
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    
    @PostMapping("/register-submit")
    public Object handleRegister(@RequestParam("username") String user,
                                 @RequestParam("password") String pass,
                                 @RequestParam("email") String email,
                                 @RequestParam("confirm_password") String confirmPass,
                                 @RequestHeader(value = "Accept", required = false) String acceptHeader) {
        
        
        boolean wantsJson = (acceptHeader != null && acceptHeader.contains("application/json"));

        
        if (!pass.equals(confirmPass)) {
            if (wantsJson) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                     .body(Map.of("status", "error", "message", "Mật khẩu không khớp!"));
            }
            return "redirect:/Register.html?error=mismatch";
        }

        
        if (userRepository.register(user, pass, email)) {
            if (wantsJson) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Đăng ký thành công!");
                return ResponseEntity.ok(response);
            }
            return "redirect:/Login.html?registered=true"; 
        } else {
            if (wantsJson) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                     .body(Map.of("status", "error", "message", "Tài khoản đã tồn tại!"));
            }
            return "redirect:/Register.html?error=exists"; 
        }
    }
}
