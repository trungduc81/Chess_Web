package com.trung.chess.Model;

import java.util.HashMap;
import java.util.Map;


public class ChessGameState {
    private final String whitePlayerId; 
    private String blackPlayerId;       
    private int whiteSeconds;           
    private int blackSeconds;           
    private long lastMoveTime;          
    private String currentTurn;         
    private int moveCount;              
    private Map<String, Object> lastMove; 
    private boolean gameEnded;          
    
    
    public ChessGameState(String whitePlayerId, int initialSeconds) {
        this.whitePlayerId = whitePlayerId;
        this.whiteSeconds = initialSeconds;
        this.blackSeconds = initialSeconds;
        this.lastMoveTime = System.currentTimeMillis();
        this.currentTurn = "w"; 
        this.moveCount = 0;
        this.gameEnded = false;
    }
    
    
    public void setBlackPlayerId(String playerId) {
        this.blackPlayerId = playerId;
    }
    
    
    public synchronized boolean validateAndMove(String playerId, String from, String to, String promotion) {
        
        if ("w".equals(currentTurn) && !playerId.equals(whitePlayerId)) {
            return false;
        }
        if ("b".equals(currentTurn) && !playerId.equals(blackPlayerId)) {
            return false;
        }
        
        
        if (from == null || to == null || from.length() != 2 || to.length() != 2) {
            return false;
        }
        
        
        lastMove = new HashMap<>();
        lastMove.put("from", from);
        lastMove.put("to", to);
        lastMove.put("promotion", promotion);
        
        
        if (moveCount > 0) {
            updateTimer();
        } else {
            
            lastMoveTime = System.currentTimeMillis();
        }
        
        
        currentTurn = "w".equals(currentTurn) ? "b" : "w";
        moveCount++;
        
        return true;
    }
    
    
    private void updateTimer() {
        long now = System.currentTimeMillis();
        long elapsed = (now - lastMoveTime) / 1000; 
        
        
        if ("w".equals(currentTurn)) {
            whiteSeconds -= elapsed;
            if (whiteSeconds < 0) whiteSeconds = 0;
        } else {
            blackSeconds -= elapsed;
            if (blackSeconds < 0) blackSeconds = 0;
        }
        
        lastMoveTime = now; 
    }
    
    
    public int getWhiteSeconds() {
        return whiteSeconds;
    }
    
    public int getBlackSeconds() {
        return blackSeconds;
    }
    
    
    public boolean isTimeExpired() {
        return whiteSeconds <= 0 || blackSeconds <= 0;
    }
    
    
    public String getTimeExpiredResult() {
        if (whiteSeconds <= 0) return "Black wins on time";
        if (blackSeconds <= 0) return "White wins on time";
        return null;
    }
    
    public String getCurrentTurn() {
        return currentTurn;
    }
    
    public int getMoveCount() {
        return moveCount;
    }

    public String getWhitePlayerId() {
        return whitePlayerId;
    }

    public String getBlackPlayerId() {
        return blackPlayerId;
    }

    
    public synchronized boolean markGameEnded() {
        if (gameEnded) {
            return false; 
        }
        gameEnded = true;
        return true;
    }

    
    public synchronized boolean isGameEnded() {
        return gameEnded;
    }
}

