




CREATE DATABASE IF NOT EXISTS chessmaster CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chessmaster;

DROP TABLE IF EXISTS guest_matches;
DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS users;



CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role ENUM('ADMIN', 'USER') DEFAULT 'USER',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE rooms (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_code VARCHAR(20) NOT NULL UNIQUE,
    host_id INT NOT NULL,
    guest_id INT NULL,
    status ENUM('WAITING', 'PLAYING', 'FINISHED') DEFAULT 'WAITING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (host_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (guest_id) REFERENCES users(id) ON DELETE SET NULL
);



CREATE TABLE matches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NULL,
    white_player_id INT NOT NULL,
    black_player_id INT NOT NULL,
    result ENUM('WHITE_WIN', 'BLACK_WIN', 'DRAW') NOT NULL,
    reason VARCHAR(100),
    pgn TEXT,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL,
    FOREIGN KEY (white_player_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (black_player_id) REFERENCES users(id) ON DELETE CASCADE
);




CREATE TABLE guest_matches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE INDEX idx_rooms_status ON rooms(status);
CREATE INDEX idx_rooms_code ON rooms(room_code);
CREATE INDEX idx_matches_white ON matches(white_player_id);
CREATE INDEX idx_matches_black ON matches(black_player_id);
CREATE INDEX idx_matches_played_at ON matches(played_at);






INSERT INTO users (username, password, email, role, active) VALUES
('admin', '$2a$10$Up3iuT5AgrA82ZyK1CUShey1Bh.a8Zgn4hnOzRwY4.ZdFU.qpiHW6', 'admin@chess.local', 'ADMIN', TRUE),
('player1', '$2a$10$ir/kCeRRYW7EE1FVUXdlRuMjqIdT/pByajjaxAnxQ8s8M59sAOAuW', 'player1@chess.local', 'USER', TRUE),
('player2', '$2a$10$ir/kCeRRYW7EE1FVUXdlRuMjqIdT/pByajjaxAnxQ8s8M59sAOAuW', 'player2@chess.local', 'USER', TRUE);



SELECT 'Database setup completed!' AS status;
SELECT id, username, email, role, active FROM users;

