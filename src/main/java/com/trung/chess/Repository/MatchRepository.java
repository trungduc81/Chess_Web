package com.trung.chess.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class MatchRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    
    public void saveMatch(Integer roomId, int whitePlayerId, int blackPlayerId, String result, String reason, String pgn) {
        String sql = "INSERT INTO matches (room_id, white_player_id, black_player_id, result, reason, pgn) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, roomId, whitePlayerId, blackPlayerId, result, reason, pgn);
    }

    
    public void saveMatchByUsername(String whiteUsername, String blackUsername, String resultForWhite, String reason, String pgn) {
        Integer whiteId = userRepository.findUserIdByUsername(whiteUsername);
        Integer blackId = userRepository.findUserIdByUsername(blackUsername);
        
        if (whiteId == null || blackId == null) {
            return; 
        }
        
        
        String dbResult;
        if ("Thắng".equalsIgnoreCase(resultForWhite)) {
            dbResult = "WHITE_WIN";
        } else if ("Thua".equalsIgnoreCase(resultForWhite)) {
            dbResult = "BLACK_WIN";
        } else {
            dbResult = "DRAW";
        }
        
        saveMatch(null, whiteId, blackId, dbResult, reason, pgn);
    }

    
    public void saveP2PMatch(String roomCode, String whiteName, String blackName, String whiteResult, String reason, String pgn) {
        Integer whiteId = userRepository.findUserIdByUsername(whiteName);
        Integer blackId = userRepository.findUserIdByUsername(blackName);
        
        
        if (whiteId == null && blackId == null) {
            return;
        }
        
        
        if (whiteId == null) whiteId = blackId;
        if (blackId == null) blackId = whiteId;
        
        
        String dbResult;
        if ("Thắng".equalsIgnoreCase(whiteResult)) {
            dbResult = "WHITE_WIN";
        } else if ("Thua".equalsIgnoreCase(whiteResult)) {
            dbResult = "BLACK_WIN";
        } else {
            dbResult = "DRAW";
        }
        
        
        Integer roomId = null;
        if (roomCode != null && !roomCode.isBlank()) {
            try {
                roomId = jdbcTemplate.queryForObject(
                    "SELECT id FROM rooms WHERE room_code = ?", 
                    Integer.class, 
                    roomCode
                );
            } catch (DataAccessException e) {
                
            }
        }
        
        saveMatch(roomId, whiteId, blackId, dbResult, reason, pgn);
    }

    
    public void saveGameHistoryWithPgn(String roomCode, String username, String opponent, String result, String reason, String pgn) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            return; 
        }
        
        
        Integer opponentId = userRepository.findUserIdByUsername(opponent);
        
        
        int blackPlayerId = opponentId != null ? opponentId : userId;
        
        
        String dbResult;
        if ("Thắng".equalsIgnoreCase(result)) {
            dbResult = "WHITE_WIN";
        } else if ("Thua".equalsIgnoreCase(result)) {
            dbResult = "BLACK_WIN";
        } else {
            dbResult = "DRAW";
        }
        
        
        String sql = "INSERT INTO matches (room_id, white_player_id, black_player_id, result, reason, pgn) VALUES (?, ?, ?, ?, ?, ?)";
        
        
        Integer roomId = null;
        if (roomCode != null && !roomCode.isBlank()) {
            try {
                roomId = jdbcTemplate.queryForObject(
                    "SELECT id FROM rooms WHERE room_code = ?", 
                    Integer.class, 
                    roomCode
                );
            } catch (DataAccessException e) {
                
            }
        }
        
        jdbcTemplate.update(sql, roomId, userId, blackPlayerId, dbResult, reason, pgn);
    }

    
    public List<Map<String, Object>> getHistory(String username) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            return new ArrayList<>();
        }
        
        String sql = """
            SELECT 
                m.id,
                DATE_FORMAT(m.played_at, '%d/%m/%Y %H:%i') as date,
                CASE 
                    WHEN m.reason LIKE '%|AI:easy%' THEN 'Máy (Dễ)'
                    WHEN m.reason LIKE '%|AI:medium%' THEN 'Máy (Trung bình)'
                    WHEN m.reason LIKE '%|AI:hard%' THEN 'Máy (Khó)'
                    WHEN m.white_player_id = m.black_player_id AND m.reason NOT LIKE '%|AI:%' THEN 'Khách (Guest)'
                    WHEN m.white_player_id = m.black_player_id THEN 'Máy (AI)'
                    WHEN m.white_player_id = ? THEN black.username 
                    ELSE white.username 
                END as opponent,
                CASE 
                    WHEN m.white_player_id = m.black_player_id AND m.result = 'WHITE_WIN' THEN 'Thắng'
                    WHEN m.white_player_id = m.black_player_id AND m.result = 'BLACK_WIN' THEN 'Thua'
                    WHEN m.white_player_id = m.black_player_id AND m.result = 'DRAW' THEN 'Hòa'
                    WHEN m.result = 'WHITE_WIN' AND m.white_player_id = ? THEN 'Thắng'
                    WHEN m.result = 'BLACK_WIN' AND m.black_player_id = ? THEN 'Thắng'
                    WHEN m.result = 'WHITE_WIN' AND m.black_player_id = ? THEN 'Thua'
                    WHEN m.result = 'BLACK_WIN' AND m.white_player_id = ? THEN 'Thua'
                    ELSE 'Hòa'
                END as result,
                CASE 
                    WHEN m.reason LIKE '%|AI:%' THEN SUBSTRING_INDEX(m.reason, '|AI:', 1)
                    ELSE m.reason
                END as reason,
                CASE WHEN m.pgn IS NOT NULL AND m.pgn != '' THEN 1 ELSE 0 END as has_pgn
            FROM matches m
            JOIN users white ON m.white_player_id = white.id
            JOIN users black ON m.black_player_id = black.id
            WHERE m.white_player_id = ? OR m.black_player_id = ?
            ORDER BY m.played_at DESC
            """;
        
        return jdbcTemplate.queryForList(sql, userId, userId, userId, userId, userId, userId, userId);
    }

    
    public Map<String, Object> getMatchById(Long id, String username) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            return Map.of();
        }
        
        String sql = """
            SELECT 
                m.id,
                DATE_FORMAT(m.played_at, '%d/%m/%Y %H:%i') as date,
                CASE 
                    WHEN m.white_player_id = m.black_player_id THEN white.username 
                    ELSE white.username 
                END as white_player,
                CASE 
                    WHEN m.reason LIKE '%|AI:easy%' THEN 'Máy (Dễ)'
                    WHEN m.reason LIKE '%|AI:medium%' THEN 'Máy (Trung bình)'
                    WHEN m.reason LIKE '%|AI:hard%' THEN 'Máy (Khó)'
                    WHEN m.white_player_id = m.black_player_id AND m.reason NOT LIKE '%|AI:%' THEN 'Khách (Guest)'
                    WHEN m.white_player_id = m.black_player_id THEN 'Máy (AI)'
                    ELSE black.username 
                END as black_player,
                CASE 
                    WHEN m.reason LIKE '%|AI:easy%' THEN 'Máy (Dễ)'
                    WHEN m.reason LIKE '%|AI:medium%' THEN 'Máy (Trung bình)'
                    WHEN m.reason LIKE '%|AI:hard%' THEN 'Máy (Khó)'
                    WHEN m.white_player_id = m.black_player_id AND m.reason NOT LIKE '%|AI:%' THEN 'Khách (Guest)'
                    WHEN m.white_player_id = m.black_player_id THEN 'Máy (AI)'
                    WHEN m.white_player_id = ? THEN black.username 
                    ELSE white.username 
                END as opponent,
                CASE 
                    WHEN m.white_player_id = m.black_player_id AND m.result = 'WHITE_WIN' THEN 'Thắng'
                    WHEN m.white_player_id = m.black_player_id AND m.result = 'BLACK_WIN' THEN 'Thua'
                    WHEN m.white_player_id = m.black_player_id AND m.result = 'DRAW' THEN 'Hòa'
                    WHEN m.result = 'WHITE_WIN' AND m.white_player_id = ? THEN 'Thắng'
                    WHEN m.result = 'BLACK_WIN' AND m.black_player_id = ? THEN 'Thắng'
                    WHEN m.result = 'WHITE_WIN' AND m.black_player_id = ? THEN 'Thua'
                    WHEN m.result = 'BLACK_WIN' AND m.white_player_id = ? THEN 'Thua'
                    ELSE 'Hòa'
                END as result,
                CASE 
                    WHEN m.reason LIKE '%|AI:%' THEN SUBSTRING_INDEX(m.reason, '|AI:', 1)
                    ELSE m.reason
                END as reason,
                m.pgn
            FROM matches m
            JOIN users white ON m.white_player_id = white.id
            JOIN users black ON m.black_player_id = black.id
            WHERE m.id = ? AND (m.white_player_id = ? OR m.black_player_id = ?)
            """;
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId, userId, userId, userId, userId, id, userId, userId);
        if (rows.isEmpty()) {
            return Map.of();
        }
        return rows.get(0);
    }

    
    public Map<String, Object> getLatestHistory(String username) {
        List<Map<String, Object>> history = getHistory(username);
        if (history.isEmpty()) {
            return Map.of();
        }
        return history.get(0);
    }

    
    public Map<String, Object> getUserStats(String username) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            return Map.of(
                "totalMatches", 0,
                "totalWins", 0,
                "winRate", 0.0
            );
        }

        String sql = "SELECT " +
                 "COUNT(*) as total_matches, " +
                 "SUM(CASE " +
                 "  WHEN m.white_player_id = m.black_player_id AND m.white_player_id = ? AND m.result = 'WHITE_WIN' THEN 1 " +
                 "  WHEN m.white_player_id = m.black_player_id AND m.white_player_id = ? THEN 0 " +
                 "  WHEN m.white_player_id = ? AND m.result = 'WHITE_WIN' THEN 1 " +
                 "  WHEN m.black_player_id = ? AND m.result = 'BLACK_WIN' THEN 1 " +
                 "  ELSE 0 END) as total_wins " +
                 "FROM matches m " +
                 "WHERE m.white_player_id = ? OR m.black_player_id = ?";

        Map<String, Object> row = jdbcTemplate.queryForMap(sql, userId, userId, userId, userId, userId, userId);
        long totalMatches = row.get("total_matches") == null ? 0L : ((Number) row.get("total_matches")).longValue();
        long totalWins = row.get("total_wins") == null ? 0L : ((Number) row.get("total_wins")).longValue();
        double winRate = totalMatches == 0 ? 0.0 : Math.round((totalWins * 1000.0) / totalMatches) / 10.0;

        return Map.of(
            "totalMatches", totalMatches,
            "totalWins", totalWins,
            "winRate", winRate
        );
    }

    
    public List<Map<String, Object>> getHistoryByUserId(int userId) {
        String sql = """
            SELECT 
                m.id,
                DATE_FORMAT(m.played_at, '%d/%m/%Y %H:%i') as date,
                CASE 
                    WHEN m.reason LIKE '%|AI:easy%' THEN 'Máy (Dễ)'
                    WHEN m.reason LIKE '%|AI:medium%' THEN 'Máy (Trung bình)'
                    WHEN m.reason LIKE '%|AI:hard%' THEN 'Máy (Khó)'
                    WHEN m.white_player_id = m.black_player_id AND m.reason NOT LIKE '%|AI:%' THEN 'Khách (Guest)'
                    WHEN m.white_player_id = m.black_player_id THEN 'Máy (AI)'
                    WHEN m.white_player_id = ? THEN black.username 
                    ELSE white.username 
                END as opponent,
                CASE 
                    WHEN m.white_player_id = m.black_player_id AND m.result = 'WHITE_WIN' THEN 'Thắng'
                    WHEN m.white_player_id = m.black_player_id AND m.result = 'BLACK_WIN' THEN 'Thua'
                    WHEN m.white_player_id = m.black_player_id AND m.result = 'DRAW' THEN 'Hòa'
                    WHEN m.result = 'WHITE_WIN' AND m.white_player_id = ? THEN 'Thắng'
                    WHEN m.result = 'BLACK_WIN' AND m.black_player_id = ? THEN 'Thắng'
                    WHEN m.result = 'WHITE_WIN' AND m.black_player_id = ? THEN 'Thua'
                    WHEN m.result = 'BLACK_WIN' AND m.white_player_id = ? THEN 'Thua'
                    ELSE 'Hòa'
                END as result,
                CASE 
                    WHEN m.reason LIKE '%|AI:%' THEN SUBSTRING_INDEX(m.reason, '|AI:', 1)
                    ELSE m.reason
                END as reason,
                CASE WHEN m.pgn IS NOT NULL AND m.pgn != '' THEN 1 ELSE 0 END as has_pgn,
                m.pgn
            FROM matches m
            JOIN users white ON m.white_player_id = white.id
            JOIN users black ON m.black_player_id = black.id
            WHERE m.white_player_id = ? OR m.black_player_id = ?
            ORDER BY m.played_at DESC
            """;

        return jdbcTemplate.queryForList(sql, userId, userId, userId, userId, userId, userId, userId);
    }

    
    public Map<String, Object> getMatchByIdForAdmin(Long matchId) {
        String sql = """
            SELECT 
                m.id,
                DATE_FORMAT(m.played_at, '%d/%m/%Y %H:%i') as date,
                white.username as white_player,
                CASE 
                    WHEN m.reason LIKE '%|AI:easy%' THEN 'Máy (Dễ)'
                    WHEN m.reason LIKE '%|AI:medium%' THEN 'Máy (Trung bình)'
                    WHEN m.reason LIKE '%|AI:hard%' THEN 'Máy (Khó)'
                    WHEN m.white_player_id = m.black_player_id AND m.reason NOT LIKE '%|AI:%' THEN 'Khách (Guest)'
                    WHEN m.white_player_id = m.black_player_id THEN 'Máy (AI)'
                    ELSE black.username 
                END as black_player,
                m.result,
                CASE 
                    WHEN m.reason LIKE '%|AI:%' THEN SUBSTRING_INDEX(m.reason, '|AI:', 1)
                    ELSE m.reason
                END as reason,
                m.pgn
            FROM matches m
            JOIN users white ON m.white_player_id = white.id
            JOIN users black ON m.black_player_id = black.id
            WHERE m.id = ?
            """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, matchId);
        if (rows.isEmpty()) {
            return Map.of();
        }
        return rows.get(0);
    }

    
    public long countAllMatches() {
        try {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matches", Long.class);
            return total == null ? 0L : total;
        } catch (DataAccessException e) {
            return 0L;
        }
    }

    
    public long countTodayMatches() {
        try {
            Long totalToday = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matches WHERE DATE(played_at) = CURRENT_DATE()",
                Long.class
            );
            return totalToday == null ? 0L : totalToday;
        } catch (DataAccessException e) {
            return 0L;
        }
    }

    
    public void incrementGuestMatch() {
        try {
            jdbcTemplate.update("INSERT INTO guest_matches (played_at) VALUES (CURRENT_TIMESTAMP)");
        } catch (DataAccessException e) {
            
        }
    }

    
    public long countGuestMatches() {
        try {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM guest_matches", Long.class);
            return total == null ? 0L : total;
        } catch (DataAccessException e) {
            return 0L;
        }
    }

    
    public long countAllMatchesIncludingGuests() {
        return countAllMatches() + countGuestMatches();
    }

    
    public List<Map<String, Object>> getRecentMatches(int limit) {
        String sql = """
            SELECT 
                m.id,
                white.username as white_player,
                CASE 
                    WHEN m.reason LIKE '%|AI:easy%' THEN 'Máy (Dễ)'
                    WHEN m.reason LIKE '%|AI:medium%' THEN 'Máy (Trung bình)'
                    WHEN m.reason LIKE '%|AI:hard%' THEN 'Máy (Khó)'
                    WHEN m.white_player_id = m.black_player_id AND m.reason NOT LIKE '%|AI:%' THEN 'Khách (Guest)'
                    WHEN m.white_player_id = m.black_player_id THEN 'Máy (AI)'
                    ELSE black.username 
                END as black_player,
                m.result,
                CASE 
                    WHEN m.reason LIKE '%|AI:%' THEN SUBSTRING_INDEX(m.reason, '|AI:', 1)
                    ELSE m.reason
                END as reason,
                DATE_FORMAT(m.played_at, '%d/%m/%Y %H:%i') as played_at,
                CASE WHEN m.pgn IS NOT NULL AND m.pgn != '' THEN 1 ELSE 0 END as has_pgn
            FROM matches m
            JOIN users white ON m.white_player_id = white.id
            JOIN users black ON m.black_player_id = black.id
            ORDER BY m.played_at DESC
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, limit);
    }

    
    public List<Map<String, Object>> getTopPlayersByWinRate(int limit) {
        String sql = "SELECT u.id, u.username, " +
                     "s.total_matches, s.total_wins, " +
                     "ROUND((s.total_wins * 100.0) / s.total_matches, 1) as win_rate " +
                     "FROM users u " +
                     "JOIN (" +
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
                     "WHERE s.total_matches > 0 " +
                     "ORDER BY win_rate DESC, s.total_wins DESC, s.total_matches DESC, u.username ASC " +
                     "LIMIT ?";
        return jdbcTemplate.queryForList(sql, limit);
    }
}
