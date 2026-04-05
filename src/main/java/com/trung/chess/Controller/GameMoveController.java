package com.trung.chess.Controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trung.chess.Model.ChessGameState;
import com.trung.chess.Repository.MatchRepository;
import com.trung.chess.Service.RoomService;


@Controller
public class GameMoveController {
    private static final Logger logger = LoggerFactory.getLogger(GameMoveController.class);

    
    private final SimpMessagingTemplate messagingTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RoomService roomService; 

    @Autowired
    private MatchRepository matchRepository; 

    public GameMoveController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    
    @MessageMapping("/join/{roomCode}")
    public void joinRoom(@DestinationVariable String roomCode, @Payload String payload, 
                        SimpMessageHeaderAccessor headerAccessor) {
        try {
            
            JsonNode json = objectMapper.readTree(payload);
            
            
            if (!json.has("playerId") || json.get("playerId").asText().isEmpty()) {
                logger.warn("Join room request missing playerId for room: {}", roomCode);
                sendError(roomCode, "", "Missing playerId");
                return;
            }
            
            
            String playerId = json.get("playerId").asText();
            int initialSeconds = json.has("initialSeconds") ? json.get("initialSeconds").asInt() : 300;
            String displayName = json.has("displayName") && !json.get("displayName").asText().isBlank()
                ? json.get("displayName").asText()
                : "Guest";
            boolean isGuest = json.has("isGuest") && json.get("isGuest").asBoolean();
            
            
            String color = roomService.assignColor(roomCode, playerId, initialSeconds, displayName, isGuest);
            
            
            Map<String, Object> response = new HashMap<>();
            response.put("color", color);
            response.put("playerId", playerId);
            response.put("whiteName", roomService.getWhiteDisplayName(roomCode));
            response.put("blackName", roomService.getBlackDisplayName(roomCode));
            response.put("whiteGuest", roomService.isWhiteGuest(roomCode));
            response.put("blackGuest", roomService.isBlackGuest(roomCode));
            
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/role", objectMapper.writeValueAsString(response));
            
            
            if ("b".equals(color)) {
                ChessGameState gameState = roomService.getGameState(roomCode);
                Map<String, Object> readyMessage = new HashMap<>();
                readyMessage.put("action", "game_ready");
                readyMessage.put("whiteSeconds", gameState != null ? gameState.getWhiteSeconds() : initialSeconds);
                readyMessage.put("blackSeconds", gameState != null ? gameState.getBlackSeconds() : initialSeconds);
                messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/action", objectMapper.writeValueAsString(readyMessage));
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing join room payload for room: {}", roomCode, e);
            sendError(roomCode, "", "Invalid request format");
        } catch (Exception e) {
            logger.error("Error in joinRoom for room: {}", roomCode, e);
        }
    }

    
    @MessageMapping("/move/{roomCode}")
    public void handleMove(@DestinationVariable String roomCode, @Payload String moveData) {
        try {
            JsonNode json = objectMapper.readTree(moveData);
            
            
            if (!json.has("playerId") || !json.has("data")) {
                logger.warn("Invalid move request for room: {}", roomCode);
                return;
            }
            
            String playerId = json.get("playerId").asText();
            JsonNode moveNode = json.get("data");
            
            
            if (!moveNode.has("from") || !moveNode.has("to")) {
                sendError(roomCode, playerId, "Missing move coordinates");
                return;
            }
            
            String from = moveNode.get("from").asText();
            String to = moveNode.get("to").asText();
            String promotion = moveNode.has("promotion") ? moveNode.get("promotion").asText() : null;
            
            
            ChessGameState gameState = roomService.getGameState(roomCode);
            if (gameState == null) {
                sendError(roomCode, playerId, "Game not found");
                return;
            }
            
            
            boolean isFirstMove = gameState.getMoveCount() == 0;
            
            
            boolean valid = gameState.validateAndMove(playerId, from, to, promotion);
            
            if (!valid) {
                sendError(roomCode, playerId, "Invalid move");
                return;
            }
            
            
            if (isFirstMove) {
                Map<String, Object> startMessage = new HashMap<>();
                startMessage.put("action", "game_start");
                startMessage.put("whiteSeconds", gameState.getWhiteSeconds());
                startMessage.put("blackSeconds", gameState.getBlackSeconds());
                messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/action", objectMapper.writeValueAsString(startMessage));
            }
            
            
            Map<String, Object> response = new HashMap<>();
            response.put("playerId", playerId);
            response.put("move", moveNode);
            response.put("whiteSeconds", gameState.getWhiteSeconds());
            response.put("blackSeconds", gameState.getBlackSeconds());
            
            messagingTemplate.convertAndSend("/topic/room/" + roomCode, objectMapper.writeValueAsString(response));
            
            
            if (gameState.isTimeExpired()) {
                if (gameState.markGameEnded()) {
                    boolean whiteExpired = gameState.getWhiteSeconds() <= 0;
                    String whiteResult = whiteExpired ? "Thua" : "Thắng";
                    String blackResult = whiteExpired ? "Thắng" : "Thua";
                    broadcastAndPersistGameOver(roomCode, "Hết giờ", whiteResult, blackResult, null);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing move data for room: {}", roomCode, e);
        } catch (Exception e) {
            logger.error("Error in handleMove for room: {}", roomCode, e);
        }
    }

    
    @MessageMapping("/resign/{roomCode}")
    public void handleResign(@DestinationVariable String roomCode, @Payload String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            if (!json.has("playerId")) {
                sendError(roomCode, "", "Missing playerId");
                return;
            }

            String playerId = json.get("playerId").asText();
            String pgn = json.has("pgn") ? json.get("pgn").asText() : null;
            ChessGameState gameState = roomService.getGameState(roomCode);
            if (gameState == null) {
                sendError(roomCode, playerId, "Game not found");
                return;
            }

            if (!gameState.markGameEnded()) {
                return;
            }

            String whitePlayerId = roomService.getWhitePlayerId(roomCode);
            String blackPlayerId = roomService.getBlackPlayerId(roomCode);

            if (playerId.equals(whitePlayerId)) {
                broadcastAndPersistGameOver(roomCode, "Đầu hàng", "Thua", "Thắng", pgn);
                return;
            }

            if (playerId.equals(blackPlayerId)) {
                broadcastAndPersistGameOver(roomCode, "Đầu hàng", "Thắng", "Thua", pgn);
                return;
            }

            sendError(roomCode, playerId, "Invalid player");
        } catch (JsonProcessingException e) {
            logger.error("Error parsing resign payload for room: {}", roomCode, e);
        } catch (Exception e) {
            logger.error("Error in handleResign for room: {}", roomCode, e);
        }
    }

    
    @MessageMapping("/game-over/{roomCode}")
    public void handleGameOver(@DestinationVariable String roomCode, @Payload String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            ChessGameState gameState = roomService.getGameState(roomCode);
            if (gameState == null) {
                return;
            }

            if (!gameState.markGameEnded()) {
                return;
            }

            String resultType = json.has("resultType") ? json.get("resultType").asText() : "DRAW";
            String reason = json.has("reason") ? json.get("reason").asText() : "Trận đấu kết thúc";
            String pgn = json.has("pgn") ? json.get("pgn").asText() : null;

            if ("CHECKMATE".equalsIgnoreCase(resultType)) {
                String winnerColor = json.has("winnerColor") ? json.get("winnerColor").asText() : "";
                if ("w".equalsIgnoreCase(winnerColor)) {
                    broadcastAndPersistGameOver(roomCode, reason, "Thắng", "Thua", pgn);
                } else if ("b".equalsIgnoreCase(winnerColor)) {
                    broadcastAndPersistGameOver(roomCode, reason, "Thua", "Thắng", pgn);
                } else {
                    broadcastAndPersistGameOver(roomCode, reason, "Hòa", "Hòa", pgn);
                }
            } else {
                broadcastAndPersistGameOver(roomCode, reason, "Hòa", "Hòa", pgn);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing game over payload for room: {}", roomCode, e);
        } catch (Exception e) {
            logger.error("Error in handleGameOver for room: {}", roomCode, e);
        }
    }

    
    private void broadcastAndPersistGameOver(String roomCode, String reason, String whiteResult, String blackResult, String pgn) {
        try {
            Map<String, Object> endGame = new HashMap<>();
            endGame.put("action", "game_over");
            endGame.put("reason", reason);
            endGame.put("whiteResult", whiteResult);
            endGame.put("blackResult", blackResult);
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/action", objectMapper.writeValueAsString(endGame));

            saveGameHistory(roomCode, whiteResult, blackResult, reason, pgn);
            roomService.removeRoom(roomCode);
        } catch (JsonProcessingException e) {
            logger.error("Error broadcasting game over for room: {}", roomCode, e);
        }
    }

    
    private void saveGameHistory(String roomCode, String whiteResult, String blackResult, String reason, String pgn) {
        String whiteName = roomService.getWhiteDisplayName(roomCode);
        String blackName = roomService.getBlackDisplayName(roomCode);
        boolean whiteIsGuest = roomService.isWhiteGuest(roomCode);
        boolean blackIsGuest = roomService.isBlackGuest(roomCode);

        
        
        if (!whiteIsGuest || !blackIsGuest) {
            matchRepository.saveP2PMatch(roomCode, whiteName, blackName, whiteResult, reason, pgn);
        } else {
            
            matchRepository.incrementGuestMatch();
        }
    }
    
    
    private void sendError(String roomCode, String playerId, String message) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("action", "error");
            error.put("playerId", playerId);
            error.put("message", message);
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/error", objectMapper.writeValueAsString(error));
        } catch (JsonProcessingException e) {
            logger.error("Error sending error message for room: {} to player: {}", roomCode, playerId, e);
        } catch (Exception e) {
            logger.error("Unexpected error sending error message", e);
        }
    }

    
    
    @MessageMapping("/chat/{roomCode}")
    public void handleChat(@DestinationVariable String roomCode, @Payload String chatData) {
        try {
            
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/chat", chatData);
        } catch (Exception e) {
            logger.error("Error handling chat for room: {}", roomCode, e);
        }
    }

    
    
    @MessageMapping("/draw-offer/{roomCode}")
    public void handleDrawOffer(@DestinationVariable String roomCode, @Payload String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            if (!json.has("playerId")) {
                return;
            }
            
            String playerId = json.get("playerId").asText();
            String senderName = roomService.getDisplayNameByPlayerId(roomCode, playerId);
            
            
            Map<String, Object> response = new HashMap<>();
            response.put("action", "draw_offer");
            response.put("fromPlayerId", playerId);
            response.put("fromName", senderName);
            
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/action", objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            logger.error("Error parsing draw offer for room: {}", roomCode, e);
        } catch (Exception e) {
            logger.error("Error handling draw offer for room: {}", roomCode, e);
        }
    }

    
    
    @MessageMapping("/draw-accept/{roomCode}")
    public void handleDrawAccept(@DestinationVariable String roomCode, @Payload String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String pgn = json.has("pgn") ? json.get("pgn").asText() : null;
            
            ChessGameState gameState = roomService.getGameState(roomCode);
            if (gameState == null) {
                return;
            }
            
            if (!gameState.markGameEnded()) {
                return;
            }
            
            
            broadcastAndPersistGameOver(roomCode, "Đồng ý hòa", "Hòa", "Hòa", pgn);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing draw accept for room: {}", roomCode, e);
        } catch (Exception e) {
            logger.error("Error handling draw accept for room: {}", roomCode, e);
        }
    }

    
    
    @MessageMapping("/draw-decline/{roomCode}")
    public void handleDrawDecline(@DestinationVariable String roomCode, @Payload String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String playerId = json.has("playerId") ? json.get("playerId").asText() : "";
            String declineName = roomService.getDisplayNameByPlayerId(roomCode, playerId);
            
            
            Map<String, Object> response = new HashMap<>();
            response.put("action", "draw_declined");
            response.put("byPlayerId", playerId);
            response.put("byName", declineName);
            
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/action", objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            logger.error("Error parsing draw decline for room: {}", roomCode, e);
        } catch (Exception e) {
            logger.error("Error handling draw decline for room: {}", roomCode, e);
        }
    }
}
