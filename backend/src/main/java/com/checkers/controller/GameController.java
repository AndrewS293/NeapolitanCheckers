package com.checkers.controller;

import com.checkers.database.GameDAO;
import com.checkers.database.MoveDAO;
import com.checkers.logic.GameLogic;
import com.checkers.logic.GameReplay;
import com.checkers.websocket.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final GameSessionManager sessionManager;
    public GameController(GameSessionManager sessionManager){
        this.sessionManager=sessionManager;
}

    @PostMapping
    public ResponseEntity<?> createGame(@RequestParam("visibility") GameVisibility visibility,
            @RequestParam(value="variantId",defaultValue="AMERICAN") String variantId){
                
        if(visibility==null)return ResponseEntity.badRequest().body(Map.of("error","Game visibility is required."));
        if(variantId==null||variantId.isBlank()||!sessionManager.isSupportedVariant(variantId))
            return ResponseEntity.badRequest().body(Map.of("error","Unsupported game variant: "+variantId));
        String gameId=UUID.randomUUID().toString().substring(0,6).toUpperCase(Locale.ROOT);
        String joinCode=visibility==GameVisibility.PRIVATE?UUID.randomUUID().toString().substring(0,6).toUpperCase(Locale.ROOT):null;
        long dbId=GameDAO.createGame(gameId,variantId);
        if(dbId<=0)return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","Failed to create game in database."));
        try{
            GameRoom room=sessionManager.createRoom(gameId,dbId,visibility,joinCode,variantId);
            Map<String,Object> result=new LinkedHashMap<>();result.put("gameId",room.getGameId());result.put("databaseGameId",dbId);
            if(joinCode!=null)result.put("joinCode",joinCode);result.put("status",room.getStatus().name());result.put("players",room.getPlayerCount());result.put("variantId",room.getVariantId());
            return ResponseEntity.ok(result);
        }catch(RuntimeException e){System.err.println("Room creation failed after DB game creation: "+e.getMessage());GameDAO.completeGame(dbId,null);return ResponseEntity.status(500).body(Map.of("error","Could not create game room."));}
    }
    @GetMapping("/open") public ResponseEntity<?> getOpenGames(){
        return ResponseEntity.ok(sessionManager.getOpenPublicGames().stream().map(r->Map.<String,Object>of("gameId",r.getGameId(),"status",r.getStatus().name(),"players",r.getPlayerCount(),"variantId",r.getVariantId())).collect(Collectors.toList()));
    }
    @PostMapping("/{gameId}/join") public ResponseEntity<?> joinPublicGame(@PathVariable String gameId){
        GameRoom r=sessionManager.getRoom(gameId);if(r==null)return ResponseEntity.status(404).body(Map.of("error","Game not found"));
        if(r.getVisibility()!=GameVisibility.PUBLIC)return ResponseEntity.status(403).body(Map.of("error","Game is not public"));
        if(r.isFull())return ResponseEntity.status(409).body(Map.of("error","Game is full"));return roomSummary(r);
    }
    @PostMapping("/join/{joinCode}") public ResponseEntity<?> joinPrivateGame(@PathVariable String joinCode){
        GameRoom r=sessionManager.getRoomByJoinCode(joinCode);if(r==null)return ResponseEntity.status(404).body(Map.of("error","Invalid join code"));
        if(r.isFull())return ResponseEntity.status(409).body(Map.of("error","Game is full"));return roomSummary(r);
    }
    @GetMapping("/{gameId}") public ResponseEntity<?> getGame(@PathVariable String gameId){GameRoom r=sessionManager.getRoom(gameId);return r==null?ResponseEntity.status(404).body(Map.of("error","Game not found")):roomSummary(r);}
    private ResponseEntity<?> roomSummary(GameRoom r){return ResponseEntity.ok(Map.of("gameId",r.getGameId(),"databaseGameId",r.getDatabaseGameId(),"status",r.getStatus().name(),"players",r.getPlayerCount(),"variantId",r.getVariantId()));}

    @PostMapping("/local/complete")
    public ResponseEntity<?> completeLocalGame(
            @RequestBody JsonNode request,
            HttpSession session) {
        Object identity = session.getAttribute("userId");
        if (!(identity instanceof Number) || ((Number) identity).longValue() <= 0) {
            return ResponseEntity.status(401).body(Map.of("error", "Please log in first."));
        }
        if (request == null || !request.isObject()) {
            return ResponseEntity.badRequest().body(Map.of("error", "A completed game is required."));
        }

        String variantId = request.path("variantId").asText("");
        String opponent = request.path("opponent").asText("");
        String result = request.path("result").asText("");
        JsonNode moves = request.path("moves");
        JsonNode winnerNode = request.path("winnerPlayer");
        Integer winnerPlayer = winnerNode.isIntegralNumber() && winnerNode.canConvertToInt()
                ? winnerNode.asInt() : null;
        if (!sessionManager.isSupportedVariant(variantId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unsupported game variant."));
        }
        if (!"HUMAN".equals(opponent) && !"AI".equals(opponent)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid local game opponent."));
        }
        if (!"RED_WON".equals(result) && !"BLACK_WON".equals(result)
                && !"DRAW".equals(result)) {
            return ResponseEntity.badRequest().body(Map.of("error", "A completed game result is required."));
        }
        if (!winnerNode.isMissingNode() && !winnerNode.isNull()
                && (winnerPlayer == null || (winnerPlayer != 1 && winnerPlayer != 2))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Winner must be player 1, player 2, or null."));
        }
        Integer expectedWinner = switch (result) {
            case "RED_WON" -> 1;
            case "BLACK_WON" -> 2;
            default -> null;
        };
        if (!Objects.equals(winnerPlayer, expectedWinner)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Winner does not match the game result."));
        }
        if (!moves.isArray() || moves.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Completed games must include their moves."));
        }

        List<Map<String, Object>> savedMoves = new ArrayList<>();
        for (JsonNode move : moves) {
            if (!move.isObject()
                    || !move.path("start").canConvertToInt()
                    || !move.path("end").canConvertToInt()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Each move must include valid start and end indexes."));
            }
            savedMoves.add(JSON.convertValue(
                    move, new TypeReference<Map<String, Object>>() {}));
        }
        if (!result.equals(savedMoves.getLast().get("gameState"))) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "The final saved move does not match the game result."));
        }

        long userId = ((Number) identity).longValue();
        long databaseGameId = GameDAO.createGame(
                "L" + UUID.randomUUID().toString()
                        .substring(0, 5).toUpperCase(Locale.ROOT),
                variantId);
        if (databaseGameId <= 0) {
            return ResponseEntity.status(503).body(Map.of("error", "Could not create the local game record."));
        }
        if (!GameDAO.registerPlayer(databaseGameId, userId, 1)) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Could not register the game owner.",
                    "databaseGameId", databaseGameId));
        }
        for (int i = 0; i < savedMoves.size(); i++) {
            if (!MoveDAO.saveMove(databaseGameId, userId,
                    i + 1, savedMoves.get(i))) {
                return ResponseEntity.status(503).body(Map.of(
                        "error", "Could not save the complete move history.",
                        "databaseGameId", databaseGameId));
            }
        }

        Long winnerUserId = winnerPlayer == null
                || ("AI".equals(opponent) && winnerPlayer == 2)
                ? null : userId;
        if (!GameDAO.completeGame(databaseGameId, winnerUserId)) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "The game was saved, but completion was not confirmed.",
                    "databaseGameId", databaseGameId));
        }
        return ResponseEntity.ok(Map.of(
                "databaseGameId", databaseGameId,
                "status", "COMPLETED"));
    }

    @GetMapping("/{databaseGameId}/replay")
    public ResponseEntity<?> getReplay(@PathVariable long databaseGameId,@RequestParam(value="position",required=false) Integer position,HttpSession session){
        Object identity=session.getAttribute("userId");if(!(identity instanceof Number)||((Number)identity).longValue()<=0)return ResponseEntity.status(401).body(Map.of("error","Please log in first."));
        if(databaseGameId<=0)return ResponseEntity.badRequest().body(Map.of("error","Invalid database game ID."));
        long userId=((Number)identity).longValue();
        try{
            if(!GameDAO.isParticipant(databaseGameId,userId))return ResponseEntity.status(403).body(Map.of("error","Replay is available only to players in this game."));
            JsonNode metadata=GameDAO.getGame(databaseGameId);if(metadata==null)return ResponseEntity.status(404).body(Map.of("error","Game not found."));
            var moves=MoveDAO.getMovesForReplay(databaseGameId);
            if(position==null)return ResponseEntity.ok(Map.of("databaseGameId",databaseGameId,"variantId",metadata.path("variant_id").asText("AMERICAN"),"status",metadata.path("status").asText(),"moveCount",moves.size(),"moves",moves));
            if(position<0||position>moves.size())return ResponseEntity.badRequest().body(Map.of("error","Position must be between 0 and "+moves.size()+"."));
            String variant=metadata.path("variant_id").asText("AMERICAN");
            GameLogic initial=sessionManager.createGameLogic(variant);
            GameReplay replay=new GameReplay(moves,initial);GameLogic frame=replay.seek(position);
            int[] board=new int[frame.getBoard().getCellCount()];for(int i=0;i<board.length;i++)board[i]=frame.getBoard().getPiece(i).getType();
            Map<String,Object> response=new LinkedHashMap<>();response.put("databaseGameId",databaseGameId);response.put("variantId",variant);response.put("position",position);response.put("moveCount",replay.getMoveCount());response.put("board",board);response.put("currentPlayer",frame.getCurrentPlayer());response.put("forcedJumpIndex",frame.getForcedJumpIndex());response.put("gameState",frame.getGameState().name());return ResponseEntity.ok(response);
        }catch(IllegalArgumentException e){return ResponseEntity.status(409).body(Map.of("error","This saved game cannot be reconstructed: "+e.getMessage()));}
         catch(IllegalStateException e){System.err.println("Replay retrieval failed for game "+databaseGameId+": "+e.getMessage());return ResponseEntity.status(503).body(Map.of("error","Replay is temporarily unavailable."));}
    }
}
