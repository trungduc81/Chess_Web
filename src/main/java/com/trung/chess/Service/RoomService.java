package com.trung.chess.Service;


import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.trung.chess.Model.ChessGameState;

@Service
public class RoomService {
    private final ConcurrentHashMap<String, Integer> roomOccupancy = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> roomWhiteDisplayName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> roomBlackDisplayName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> roomWhiteGuest = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> roomBlackGuest = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> roomWhitePlayerId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> roomBlackPlayerId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ChessGameState> gameStates = new ConcurrentHashMap<>();

    
    public synchronized String assignColor(String roomCode, String playerId, int initialSeconds, String displayName, boolean isGuest) {
        int count = roomOccupancy.getOrDefault(roomCode, 0);
        if (count == 0) {
            roomOccupancy.put(roomCode, 1);
            roomWhiteDisplayName.put(roomCode, displayName);
            roomWhiteGuest.put(roomCode, isGuest);
            roomWhitePlayerId.put(roomCode, playerId);
            ChessGameState gameState = new ChessGameState(playerId, initialSeconds);
            gameStates.put(roomCode, gameState);
            
            return "w"; 
        } else if (count == 1) {
            roomOccupancy.put(roomCode, 2);
            roomBlackDisplayName.put(roomCode, displayName);
            roomBlackGuest.put(roomCode, isGuest);
            roomBlackPlayerId.put(roomCode, playerId);
            ChessGameState gameState = gameStates.get(roomCode);
            if (gameState != null) {
                gameState.setBlackPlayerId(playerId);
            } else {
                gameState = new ChessGameState(playerId, initialSeconds);
                gameStates.put(roomCode, gameState);
            }
            
            return "b"; 
        }
        return "viewer"; 
    }
    
    
    public ChessGameState getGameState(String roomCode) {
        return gameStates.get(roomCode);
    }

    
    public String getWhiteDisplayName(String roomCode) {
        return roomWhiteDisplayName.getOrDefault(roomCode, "Guest");
    }

    
    public String getBlackDisplayName(String roomCode) {
        return roomBlackDisplayName.getOrDefault(roomCode, "Guest");
    }

    
    public boolean isWhiteGuest(String roomCode) {
        return roomWhiteGuest.getOrDefault(roomCode, true);
    }

    
    public boolean isBlackGuest(String roomCode) {
        return roomBlackGuest.getOrDefault(roomCode, true);
    }

    
    public String getWhitePlayerId(String roomCode) {
        return roomWhitePlayerId.get(roomCode);
    }

    
    public String getBlackPlayerId(String roomCode) {
        return roomBlackPlayerId.get(roomCode);
    }

    
    public String getDisplayNameByPlayerId(String roomCode, String playerId) {
        if (playerId == null) {
            return "Guest";
        }
        if (playerId.equals(roomWhitePlayerId.get(roomCode))) {
            return getWhiteDisplayName(roomCode);
        }
        if (playerId.equals(roomBlackPlayerId.get(roomCode))) {
            return getBlackDisplayName(roomCode);
        }
        return "Guest";
    }

    
    public boolean isGuestByPlayerId(String roomCode, String playerId) {
        if (playerId == null) {
            return true;
        }
        if (playerId.equals(roomWhitePlayerId.get(roomCode))) {
            return isWhiteGuest(roomCode);
        }
        if (playerId.equals(roomBlackPlayerId.get(roomCode))) {
            return isBlackGuest(roomCode);
        }
        return true;
    }
    
    
    public void removeRoom(String roomCode) {
        roomOccupancy.remove(roomCode);
        gameStates.remove(roomCode);
        roomWhiteDisplayName.remove(roomCode);
        roomBlackDisplayName.remove(roomCode);
        roomWhiteGuest.remove(roomCode);
        roomBlackGuest.remove(roomCode);
        roomWhitePlayerId.remove(roomCode);
        roomBlackPlayerId.remove(roomCode);
    }
}
