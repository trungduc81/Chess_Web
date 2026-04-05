package com.trung.chess.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;


@Repository
public class UserRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private PasswordEncoder passwordEncoder; 

    
    public boolean checkLogin(String username, String password) {
        String safeUsername = username == null ? "" : username.trim();
        String safePassword = password == null ? "" : password;
        String sql = "SELECT password, active FROM users WHERE username = ?";
        
        logger.info("=== LOGIN ATTEMPT === Username: {}", safeUsername);
        
        try {
            java.util.Map<String, Object> result = jdbcTemplate.queryForMap(sql, safeUsername);
            String storedPassword = (String) result.get("password");
            Object activeObj = result.get("active");
            
            logger.info("User found. Active value: {} (type: {})", activeObj, activeObj != null ? activeObj.getClass().getName() : "null");
            logger.info("Stored password starts with: {}", storedPassword != null ? storedPassword.substring(0, Math.min(10, storedPassword.length())) : "null");
            
            
            
            boolean isActive;
            if (activeObj == null) {
                isActive = true;
            } else if (activeObj instanceof Boolean) {
                isActive = (Boolean) activeObj;
            } else if (activeObj instanceof Number) {
                isActive = ((Number) activeObj).intValue() == 1;
            } else {
                isActive = true;
            }
            
            logger.info("isActive resolved to: {}", isActive);
            
            if (storedPassword == null || storedPassword.isBlank()) {
                logger.warn("Login failed: password is null or blank");
                return false;
            }
            
            
            if (!isActive) {
                logger.warn("Login failed: account is inactive");
                return false;
            }

            
            if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                boolean matches = passwordEncoder.matches(safePassword, storedPassword);
                logger.info("BCrypt password match result: {}", matches);
                return matches;
            }

            
            if (storedPassword.equals(safePassword)) {
                String upgradedHash = passwordEncoder.encode(safePassword);
                jdbcTemplate.update("UPDATE users SET password = ? WHERE username = ?", upgradedHash, safeUsername);
                logger.info("Legacy password matched, upgraded to BCrypt");
                return true;
            }

            logger.warn("Login failed: password mismatch");
            return false;
        } catch (DataAccessException e) {
            logger.error("Login failed: database error - {}", e.getMessage());
            return false;
        }
    }

    
    public boolean register(String username, String password, String email) {
        try {
            String hashedPassword = passwordEncoder.encode(password);
            String sql = "INSERT INTO users (username, password, email, active) VALUES (?, ?, ?, 1)";
            int rows = jdbcTemplate.update(sql, username, hashedPassword, email);
            return rows > 0;
        } catch (DataAccessException e) {
            return false;
        }
    }

    
    public Integer findUserIdByUsername(String username) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?", 
                Integer.class, 
                username
            );
        } catch (DataAccessException e) {
            return null;
        }
    }

    
    public String findUsernameById(int userId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT username FROM users WHERE id = ?", 
                String.class, 
                userId
            );
        } catch (DataAccessException e) {
            return null;
        }
    }

    
    public java.util.List<java.util.Map<String, Object>> findAll() {
        String sql = "SELECT u.id, u.username, u.email, " +
                     "CASE WHEN u.active IS NULL THEN 1 ELSE u.active END as active, " +
                     "DATE_FORMAT(u.created_at, '%d/%m/%Y %H:%i') as created_at, " +
                     "COALESCE(s.total_matches, 0) as total_matches, " +
                     "COALESCE(s.total_wins, 0) as total_wins, " +
                     "CASE " +
                     "  WHEN COALESCE(s.total_matches, 0) = 0 THEN 0 " +
                     "  ELSE ROUND((COALESCE(s.total_wins, 0) * 100.0) / s.total_matches, 1) " +
                     "END as win_rate " +
                     "FROM users u " +
                     "LEFT JOIN (" +
                     "  SELECT p.user_id, COUNT(*) as total_matches, SUM(p.is_win) as total_wins " +
                     "  FROM (" +
                     "    SELECT m.white_player_id as user_id, " +
                     "           CASE WHEN m.result = 'WHITE_WIN' THEN 1 ELSE 0 END as is_win " +
                     "    FROM matches m " +
                     "    UNION ALL " +
                     "    SELECT m.black_player_id as user_id, " +
                     "           CASE WHEN m.result = 'BLACK_WIN' THEN 1 ELSE 0 END as is_win " +
                     "    FROM matches m " +
                     "    WHERE m.black_player_id <> m.white_player_id " +
                     "  ) p " +
                     "  GROUP BY p.user_id " +
                     ") s ON s.user_id = u.id " +
                     "ORDER BY u.id DESC";
        return jdbcTemplate.queryForList(sql);
    }

    
    public long countAllUsers() {
        try {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
            return total == null ? 0L : total;
        } catch (DataAccessException e) {
            return 0L;
        }
    }

    
    public boolean lockUser(long userId) {
        try {
            int updated = jdbcTemplate.update("UPDATE users SET active = 0 WHERE id = ?", userId);
            return updated > 0;
        } catch (DataAccessException e) {
            return false;
        }
    }

    
    public boolean unlockUser(long userId) {
        try {
            int updated = jdbcTemplate.update("UPDATE users SET active = 1 WHERE id = ?", userId);
            return updated > 0;
        } catch (DataAccessException e) {
            return false;
        }
    }

    
    public boolean verifyUsernameEmail(String username, String email) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND email = ? AND active = 1";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, username.trim(), email.trim());
            return count != null && count > 0;
        } catch (DataAccessException e) {
            logger.error("Verify username/email failed: {}", e.getMessage());
            return false;
        }
    }

    
    public boolean resetPassword(String username, String email, String newPassword) {
        try {
            String hashedPassword = passwordEncoder.encode(newPassword);
            String sql = "UPDATE users SET password = ? WHERE username = ? AND email = ? AND active = 1";
            int updated = jdbcTemplate.update(sql, hashedPassword, username.trim(), email.trim());
            logger.info("Password reset for user '{}': {}", username, updated > 0 ? "SUCCESS" : "FAILED");
            return updated > 0;
        } catch (DataAccessException e) {
            logger.error("Reset password failed: {}", e.getMessage());
            return false;
        }
    }
}
